/* eslint-disable object-curly-spacing */
/* eslint-disable require-jsdoc */ // O añade comentarios JSDoc
const {onCall, HttpsError} = require("firebase-functions/v2/https");
const admin = require("firebase-admin");
const axios = require("axios");
const {
  SecretManagerServiceClient,
} = require("@google-cloud/secret-manager");
const util = require("util");

// Inicializa Firebase Admin SDK (solo una vez)
if (admin.apps.length === 0) {
  admin.initializeApp();
}
const db = admin.firestore();
const storage = admin.storage(); // Para admin.storage().bucket()
const secretManagerClient = new SecretManagerServiceClient();

// --- Función Auxiliar para obtener API Key de Gemini ---
/**
 * Obtiene la API Key de Gemini desde Secret Manager.
 * @return {Promise<string>} La clave API de Gemini.
 * @throws {HttpsError} Si no se puede obtener la clave.
 */
async function getGeminiApiKey() {
  // Usa 'latest' para obtener la versión más reciente del secreto.
  const name = "projects/379146167730/secrets/GEMINI_API_KEY/versions/1";
  console.log(`Accediendo al secreto de Gemini: ${name}`);
  try {
    const [version] = await secretManagerClient.accessSecretVersion({name});
    const payload = version.payload.data.toString("utf8");
    if (!payload) {
      throw new Error("Secret Manager devolvió una API Key de Gemini vacía.");
    }
    return payload;
  } catch (error) {
    console.error(
        "Error crítico al acceder a Secret Manager (Gemini):",
        error.message || error,
    );
    throw new HttpsError(
        "internal",
        "No se pudo obtener la configuración segura de Gemini.",
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
    {
      timeoutSeconds: 900,
      memory: "2GiB",
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
            console.log(
                "Datos de caché (cultivos):",
                JSON.stringify(datosExistentes.cultivos, null, 2),
            );

            let todosTienenImagenValida = true;
            if (datosExistentes.cultivos.length > 0) {
              todosTienenImagenValida = datosExistentes.cultivos.every(
                  (c, index) => {
                    const tieneUrl = c && typeof c.urlImagen === "string" &&
                      c.urlImagen && c.urlImagen.startsWith("http");
                    if (!tieneUrl) {
                      console.warn(
                          `Cultivo en caché (índice ${index},`,
                          `nombre: ${c?.nombre})`,
                          `NO tiene urlImagen válida: ${c?.urlImagen}`,
                      );
                    }
                    return tieneUrl;
                  },
              );
            }

            console.log(
                `Evaluación de caché: ¿Todos tienen imagen válida?`,
                todosTienenImagenValida,
            );

            if (todosTienenImagenValida) {
              console.log(
                  `Devolviendo ${datosExistentes.cultivos.length} cultivos`,
                  "(v2 caché).",
              );
              return {cultivos: datosExistentes.cultivos};
            } else {
              console.warn(
                  `CacheHit ${idUbicacionNorm} (v2), pero faltan URLs`,
                  "de imagen válidas. Regenerando...",
              );
            }
          } else {
            console.warn(
                `Doc ${idUbicacionNorm} (v2) existe pero 'cultivos'`,
                "es inválido o no es array. Regenerando...",
            );
          }
        }
        console.log(
            `Cache miss o datos incompletos para ${idUbicacionNorm} (v2).`,
        );

        // 4. OBTENER NOMBRES Y DESCRIPCIONES (GEMINI TEXT)
        const geminiApiKey = await getGeminiApiKey();
        const promptDescripcionesLines = [
          "Eres un asistente experto maestro en agricultura.",
          `Para la ubicación "${ubicacionOriginal}", lista los 7-10 cultivos`,
          "agrícolas más comunes y relevantes. Para cada uno, proporciona su",
          "nombre común y una descripción robusta sobre sus características",
          "en la zona, incluyendo pasos puntuales y detallados para su",
          "cultivo. Ofrece un tutorial completo y útil, con al menos 2000",
          "caracteres por cultivo.",
          "IMPORTANTE: Formatea tu respuesta EXCLUSIVAMENTE como un array",
          "JSON válido. Cada elemento debe ser un objeto con claves",
          "\"nombre\" (string) y \"descripcion\" (string).",
          "proporciona la respuesta en texto sin formato.",
          "sin ningún tipo de formato Markdown.",
          "No pongas los títulos en negrita.",
          "Evita usar dobles asteriscos para resaltar texto",
          "en texto plano, no uses ** para resaltar contenido",
          "Deja dos renglones por cada cultivo",
          "Ejemplo: [{\"nombre\":\"CultivoA\",\"descripcion\":\"Desc A.\"}]",
        ];
        const promptDescripciones = promptDescripcionesLines.join("\n").trim();

        let cultivosBase = [];
        try {
          console.log("Llamando a Gemini para descripciones (v2)...");
          const textModel = "gemini-1.5-flash-latest"; // Modelo de texto
          const textApiUrl = `https://generativelanguage.googleapis.com/v1beta/models/${textModel}:generateContent?key=${geminiApiKey}`;
          const textData = {
            contents: [{
              parts: [{text: promptDescripciones}],
            }],
            generationConfig: { // Configuración para texto
              temperature: 0.2, // Menos creatividad para datos estructurados
              // maxOutputTokens: 8192, // Modelo define el máximo
              responseMimeType: "application/json", // Esperamos JSON
            },
          };

          const responseGeminiText = await axios.post(textApiUrl, textData, {
            headers: {"Content-Type": "application/json"},
          });

          // Gemini suele devolver el JSON dentro de la parte de texto
          // cuando se le pide explícitamente formato JSON.
          const rawResponseText =
            responseGeminiText.data?.candidates?.[0]?.content?.parts?.[0]?.text;

          console.log("💬 Gemini Text raw response part (v2):", rawResponseText);

          if (rawResponseText) {
            // Intenta parsear el texto como JSON
            // Eliminar posible Markdown si Gemini aún lo añade a veces
            const jsonText = rawResponseText
                .replace(/^```json\s*/i, "")
                .replace(/```$/i, "")
                .trim();
            cultivosBase = JSON.parse(jsonText);

            if (!Array.isArray(cultivosBase)) cultivosBase = [];
            cultivosBase = cultivosBase.filter(
                (c) => c && typeof c.nombre === "string" &&
                       typeof c.descripcion === "string",
            );
            console.log(
                `Gemini (v2) devolvió ${cultivosBase.length} cultivos base.`,
            );
          } else {
            console.warn("Gemini (v2) no devolvió contenido de texto útil.");
            cultivosBase = [];
          }
        } catch (error) {
          console.error(
              "Error obteniendo descripciones de Gemini (v2):",
              util.inspect(error.response?.data || error.message, {depth: 5}),
          );
          throw new HttpsError(
              "unavailable",
              "Error al obtener datos base de cultivos con Gemini (v2).",
          );
        }

        if (cultivosBase.length === 0) {
          console.log(
              "No se obtuvieron cultivos base con Gemini. Guardando vacío.",
          );
          await docRef.set({
            nombreOriginal: ubicacionOriginal,
            cultivos: [],
            fechaConsulta: admin.firestore.FieldValue.serverTimestamp(),
          });
          return {cultivos: []};
        }

        // 5. GENERAR/SUBIR IMAGEN PARA CADA CULTIVO (GEMINI IMAGE)
        const bucket = storage.bucket();
        const cultivosFinales = [];
        const defaultImageUrl = "https://firebasestorage.googleapis."+
        "com/v0/b/anfeca2025.firebasestorage.app/o/imagenes_cultivos%2Fd"+
        "efault%2F20250509_2238_Plantas%20Hiperealistas%20Coloridas_simple_"+
        "compose_01jtw7rgqvfkc8naa5j2z56h1k.png?alt=media&token=331db839-771"+
        "0-4755-b386-b145b7bcf26c";

        // Modelo de generación de imágenes de Gemini (según tu info)
        const imageModel = "gemini-2.0-flash-preview-image-generation";
        const imageApiUrl = `https://generativelanguage.googleapis.com/v1beta/models/${imageModel}:generateContent?key=${geminiApiKey}`;

        for (const cultivoBase of cultivosBase) {
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
            continue;
          }

          try {
            console.log(
                `Procesando imagen para (v2): ${cultivoBase.nombre}`,
            );
            const promptImagen =
              `Fotografía realista de una planta de ${cultivoBase.nombre}`+
              ", en su entorno natural, lista para cosecha, colores vivos, " +
              "iluminación perfecta, toma detallada de la planta.";

            console.log(`Prompt Gemini Img`+
              `para ${cultivoBase.nombre}: "${promptImagen}"`);
            const imageDataPayload = {
              contents: [
                {
                  role: "user",
                  parts: [{ text: promptImagen }],
                },
              ],
              generationConfig: {responseModalities: ["TEXT", "IMAGE"]},
            };
            console.log("Enviando solicitud a:", imageApiUrl);
            console.log("Payload:", JSON.stringify(imageDataPayload, null, 2));

            const responseGeminiImage = await axios.post(
                imageApiUrl,
                imageDataPayload,
                { headers: { "Content-Type": "application/json" } },
            );

            console.log(
                JSON.stringify(responseGeminiImage.data, null, 2));
            const base64ImageData =
          responseGeminiImage
              .data?.candidates?.[0]?.content?.parts?.[1]?.inlineData.data;
            if (base64ImageData && base64ImageData.length > 100) {
              console.log(
                  `Img ${cultivoBase.nombre}(long: ${base64ImageData.length})`,
              );
              // Eliminar posible prefijo de data URI si Gemini lo añade
              const pureBase64 = base64ImageData.startsWith("data:") ?
                base64ImageData.substring(base64ImageData.indexOf(",") + 1) :
                base64ImageData;

              const imageBuffer = Buffer.from(pureBase64, "base64");
              console.log(
                  `Buffer${cultivoBase.nombre}(size:${imageBuffer.length})`,
              );
              const filePath =
                `imagenes_cultivos/${idUbicacionNorm}/` +
                `${nombreCultivoNorm}.png`;
              const file = bucket.file(filePath);

              console.log(`Intentando subir a Storage: ${filePath}`);
              await file.save(imageBuffer, {
                metadata: {contentType: "image/png"},
                public: true,
              });
              urlImagenCultivo = file.publicUrl();
              console.log(
                  `ImagenSubida ${cultivoBase.nombre}: ${urlImagenCultivo}`,
              );
            } else {
              console.warn(
                  `No se obtuvo base64 ${cultivoBase.nombre}.Respuesta:`,
                  JSON.stringify(responseGeminiImage.data)?.substring(0, 300),
              );
              urlImagenCultivo = defaultImageUrl;
            }
          } catch (error) {
            console.error(
                `Error img/upload Gemini (v2) para ${cultivoBase.nombre}:`,
                util.inspect(error.response?.data || error.message, {depth: 5}),
            );
            urlImagenCultivo = defaultImageUrl;
          }
          cultivosFinales.push({
            nombre: cultivoBase.nombre,
            descripcion: cultivoBase.descripcion,
            urlImagen: urlImagenCultivo,
          });
        }

        // 6. GUARDAR EN FIRESTORE
        const datosParaGuardarFinal = {
          nombreOriginal: ubicacionOriginal,
          cultivos: cultivosFinales,
          fechaConsulta: admin.firestore.FieldValue.serverTimestamp(),
        };
        await docRef.set(datosParaGuardarFinal);
        console.log(
            "Datos finales con imágenes (Gemini) guardados (v2) para: " +
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
);
