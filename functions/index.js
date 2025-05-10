/* eslint-disable object-curly-spacing */
/* eslint-disable require-jsdoc */ // O añade comentarios JSDoc
const {onCall, HttpsError} = require("firebase-functions/v2/https");
const admin = require("firebase-admin");
const axios = require("axios");
const {
  SecretManagerServiceClient,
} = require("@google-cloud/secret-manager"); // Dividido
const util = require("util");

// Inicializa Firebase Admin SDK (solo una vez)
if (admin.apps.length === 0) {
  admin.initializeApp();
}
const db = admin.firestore();
const storage = admin.storage(); // Para admin.storage().bucket()
const secretManagerClient = new SecretManagerServiceClient();

// --- Función Auxiliar para obtener API Key ---
/**
 * Obtiene la API Key de OpenAI desde Secret Manager.
 * @return {Promise<string>} La clave API.
 * @throws {HttpsError} Si no se puede obtener la clave.
 */
async function getOpenAiApiKey() {
  const name = "projects/379146167730/secrets/OPENAI_API_KEY/versions/1";
  console.log(`Accediendo al secreto: ${name}`);
  try {
    const [version] = await secretManagerClient.accessSecretVersion({name});
    const payload = version.payload.data.toString("utf8");
    if (!payload) {
      throw new Error("Secret Manager devolvió una API Key vacía.");
    }
    return payload;
  } catch (error) {
    console.error(
        "Error crítico al acceder a Secret Manager:",
        error.message || error,
    );
    throw new HttpsError(
        "internal",
        "No se pudo obtener la configuración segura.",
        error.message,
    );
  }
}

// --- Función Auxiliar para Normalizar ID ---
/**
 * Normaliza un string para usarlo como ID de Firestore/Storage.
 * @param {string} inputString El string original.
 * @return {string|null} El string normalizado o null si entrada inválida.
 */
function normalizeId(inputString) {
  if (!inputString || typeof inputString !== "string") return null;
  return inputString
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, "_")
      .replace(/_+/g, "_")
      .replace(/^_+|_+$/g, "");
}

// --- Cloud Function Principal (HTTPS Callable - 2ª Generación) ---
exports.obtenerCultivosPorUbicacion = onCall(
    { // Opciones de ejecución para 2ª gen
      timeoutSeconds: 300, // 5 minutos
      memory: "1GiB", // Nota: "GiB" para 2ª gen
    },
    async (request) => {
      console.log(
          "Función (v2) iniciada. Datos recibidos:",
          util.inspect(request.data, {depth: 3}),
      );

      const ubicacionOriginal = request.data?.ubicacion;

      console.log(
          "Valor extraído de request.data.ubicacion:",
          ubicacionOriginal,
      );
      console.log("Tipo extraído:", typeof ubicacionOriginal);

      // 1. Validar Input
      const isValidInput = ubicacionOriginal &&
                           typeof ubicacionOriginal === "string" &&
                           ubicacionOriginal.trim() !== "";
      if (!isValidInput) {
        console.error(
            "VALIDACIÓN FALLIDA (v2):",
            util.inspect(request.data, {depth: 3}),
        );
        throw new HttpsError(
            "invalid-argument",
            "Argumento \"ubicacion\" inválido o ausente en request.data.",
        );
      }
      console.log(`Validación pasada (v2): "${ubicacionOriginal}"`);

      // 2. Normalizar ID
      const idUbicacionNorm = normalizeId(ubicacionOriginal);
      if (!idUbicacionNorm) {
        console.error(`No se pudo normalizar (v2): "${ubicacionOriginal}"`);
        throw new HttpsError(
            "invalid-argument", "Formato de ubicación no válido.",
        );
      }
      console.log(`ID Ubicación Normalizado (v2): ${idUbicacionNorm}`);

      // 3. Consultar Caché (Firestore)
      const docRef = db.collection("ubicacionesCultivos").doc(idUbicacionNorm);
      try {
        const docSnap = await docRef.get();

        if (docSnap.exists) {
          console.log(`Cache hit para (v2): ${idUbicacionNorm}`);
          const datosExistentes = docSnap.data();
          if (datosExistentes && Array.isArray(datosExistentes.cultivos)) {
            const todosTienenImagen =
              datosExistentes.cultivos.length === 0 ||
              datosExistentes.cultivos.every(
                  (c) => c && typeof c.urlImagen === "string" && c.urlImagen,
              );
            if (todosTienenImagen) {
              console.log(
                  `Devolviendo ${datosExistentes.cultivos.length} cultivos`,
                  "(v2 caché).",
              );
              return {cultivos: datosExistentes.cultivos};
            } else {
              console.warn(
                  `CacheHit ${idUbicacionNorm} (v2), faltan URLs.`,
                  "Regenerando...",
              );
            }
          } else {
            console.warn(
                `Doc ${idUbicacionNorm} (v2) existe pero 'cultivos'`,
                "inválido. Regenerando...",
            );
          }
        }
        console.log(
            `Cache miss o datos incompletos para ${idUbicacionNorm} (v2).`,
        );

        // 4. OBTENER NOMBRES Y DESCRIPCIONES (GPT)
        const apiKey = await getOpenAiApiKey();
        const promptDescripcionesLines = [
          "Eres un asistente experto meastro en agricultura .",
          `Para la ubicación "${ubicacionOriginal}", lista los 7-10 cultivos`,
          "agrícolas más comunes y relevantes. Para cada uno, da su nombre",
          "común y una robusta descripción sobre su",
          "características en la zona y pasos puntuales",
          "detallando cada paso punto a punto para cultivar.",
          "Tutorial completo y util, al menos 3000 caracteres por cultivo",
          "IMPORTANTE: Formatea tu respuesta EXCLUSIVAMENTE como un array",
          "JSON válido. Cada elemento debe ser un objeto con claves",
          "\"nombre\" (string) y \"descripcion\" (string).",
          "Ejemplo: [{\"nombre\":\"CultivoA\",\"descripcion\":\"Desc A.\"}]",
        ];
        const promptDescripciones = promptDescripcionesLines.join("\n").trim();

        let cultivosBase = [];
        try {
          console.log("Llamando a GPT para descripciones (v2)...");
          const responseGpt = await axios.post(
              "https://api.openai.com/v1/chat/completions",
              {
                model: "gpt-4o-mini",
                messages: [{role: "user", content: promptDescripciones}],
                temperature: 0.5,
                max_tokens: 3200,
              },
              {headers: {
                "Authorization": `Bearer ${apiKey}`,
                "Content-Type": "application/json",
              }},
          );
          const choiceGpt = responseGpt.data?.choices?.[0];
          if (choiceGpt?.message?.content) {
            let contentGpt = choiceGpt.message.content.trim();
            const jsonMatch =
              contentGpt.match(/```json\s*([\s\S]*?)\s*```/) ||
              contentGpt.match(/```\s*([\s\S]*?)\s*```/);
            if (jsonMatch && jsonMatch[1]) contentGpt = jsonMatch[1].trim();
            else if (!contentGpt.startsWith("[") || !contentGpt.endsWith("]")) {
              console.warn("Respuesta GPT (v2) no parece JSON directo.");
            }
            cultivosBase = JSON.parse(contentGpt);
            if (!Array.isArray(cultivosBase)) cultivosBase = [];
            cultivosBase = cultivosBase.filter(
                (c) => c && typeof c.nombre === "string" &&
                       typeof c.descripcion === "string",
            );
            console.log(
                `GPT (v2) devolvió ${cultivosBase.length} cultivos base.`,
            );
          }
        } catch (error) {
          console.error(
              "Error obteniendo descripciones de GPT (v2):",
              error,
          );
          throw new HttpsError(
              "unavailable",
              "Error al obtener datos base de cultivos (v2).",
          );
        }

        if (cultivosBase.length === 0) {
          console.log(
              "No se obtuvieron cultivos base (v2). Guardando vacío.",
          );
          await docRef.set({
            nombreOriginal: ubicacionOriginal,
            cultivos: [],
            fechaConsulta: admin.firestore.FieldValue.serverTimestamp(),
          });
          return {cultivos: []};
        }

        // 5. GENERAR/SUBIR IMAGEN PARA CADA CULTIVO
        const bucket = storage.bucket(); // Bucket por defecto
        const cultivosFinales = [];
        // URL por defecto si falla DALL-E (ajusta el nombre y token)
        const defaultImageUrl = "https://firebasestorage.googleapis.com"+
                                "/v0/b/anfeca2025.firebasestorage.app/o/"+
                                "imagenes_cultivos%2Fdefault%2F20250509_2"+
                                "238_Plantas%20Hiperealistas%20Coloridas_"+
                                "simple_compose_01jtw7rgqvfkc8naa5j2z56h1k.png"+
                                "?alt=media&token=331db839-7710-4755-b386-"+
                                "b145b7bcf26c";

        await Promise.all(
            cultivosBase.map(async (cultivoBase) => {
              let urlImagenCultivo = null;
              const nombreCultivoNorm = normalizeId(cultivoBase.nombre);
              if (!nombreCultivoNorm) {
                console.warn(
                    `No se normalizó nombre (v2): ${cultivoBase.nombre}`,
                );
                cultivosFinales.push({
                  ...cultivoBase,
                  urlImagen: defaultImageUrl,
                });
                return;
              }
              try {
                console.log(
                    `Procesando imagen para (v2): ${cultivoBase.nombre}`,
                );
                const promptImagen =
                  `Fotografia real de planta de ${cultivoBase.nombre}` +
                  ", imagen llamativa, estimulate, perfecta";
                const responseDalle = await axios.post(
                    "https://api.openai.com/v1/images/generations",
                    {
                      model: "dall-e-3",
                      prompt: promptImagen,
                      n: 1,
                      size: "1024x1024",
                      response_format: "b64_json",
                    },
                    {headers: {
                      "Authorization": `Bearer ${apiKey}`,
                      "Content-Type": "application/json",
                    }},
                );
                const base64ImageData =
                  responseDalle.data?.data?.[0]?.b64_json;
                if (base64ImageData) {
                  const imageBuffer = Buffer.from(base64ImageData, "base64");
                  const filePath =
                    `imagenes_cultivos/${idUbicacionNorm}/` +
                    `${nombreCultivoNorm}.png`;
                  const file = bucket.file(filePath);
                  await file.save(imageBuffer, {
                    metadata: {contentType: "image/png"},
                    public: true,
                  });
                  urlImagenCultivo = file.publicUrl();
                  console.log(
                      `Imagen subida (v2) para ${cultivoBase.nombre}`,
                  );
                } else {
                  console.warn(
                      `No b64_json DALL-E (v2) para ${cultivoBase.nombre}`,
                  );
                  urlImagenCultivo = defaultImageUrl;
                }
              } catch (error) {
                console.error(
                    `Error img/upload (v2) para ${cultivoBase.nombre}:`,
                    error.response?.data || error.message,
                );
                urlImagenCultivo = defaultImageUrl;
              }
              cultivosFinales.push({
                nombre: cultivoBase.nombre,
                descripcion: cultivoBase.descripcion,
                urlImagen: urlImagenCultivo, // Será la generada o la default
              });
            }),
        );

        // 6. GUARDAR EN FIRESTORE
        const datosParaGuardarFinal = {
          nombreOriginal: ubicacionOriginal,
          cultivos: cultivosFinales,
          fechaConsulta: admin.firestore.FieldValue.serverTimestamp(),
        };
        await docRef.set(datosParaGuardarFinal);
        console.log(
            "Datos finales con imágenes guardados (v2) para: " +
            `${idUbicacionNorm}`,
        );

        // 7. DEVOLVER A LA APP
        return {cultivos: cultivosFinales};
      } catch (error) {
        console.error(
            `Error GRAL (v2) procesando ${ubicacionOriginal} ` +
            `(ID: ${idUbicacionNorm}):`,
            error,
        );
        if (error instanceof HttpsError) {
          throw error;
        } else {
          throw new HttpsError(
              "internal",
              "Ocurrió un error inesperado (v2).",
              error.message,
          );
        }
      }
    },
); // Fin exports.obtenerCultivosPorUbicacion (v2)

// Asegúrate de tener una línea en blanco al final del archivo
