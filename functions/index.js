const functions = require("firebase-functions");
const admin = require("firebase-admin");
const axios = require("axios"); // Para llamar a OpenAI
// eslint-disable-next-line max-len
const {SecretManagerServiceClient} = require("@google-cloud/secret-manager");

if (admin.apps.length === 0) {
  admin.initializeApp();
}

const db = admin.firestore();

const secretManagerClient = new SecretManagerServiceClient();

// --- Función Auxiliar para obtener API Key (Reutilizada) ---
/**
 * Obtiene la API Key de OpenAI desde Secret Manager.
 * @async
 * @throws {functions.https.HttpsError} Si no se puede obtener la clave.
 * @return {Promise<string>} La API Key de OpenAI.
 */
async function getOpenAiApiKey() {
  const name = "projects/anfeca2025/secrets/OPENAI_API_KEY/versions/latest";
  try {
    const [version] = await secretManagerClient.accessSecretVersion({name});
    const payload = version.payload.data.toString("utf8");
    if (!payload) {
      // eslint-disable-next-line max-len
      throw new Error("Secret Manager devolvió una API Key vacía.");
    }
    return payload;
  } catch (error) {
    // eslint-disable-next-line max-len
    console.error("Error crítico al acceder a Secret Manager:", error);
    // Lanza un error que la función callable pueda manejar
    throw new functions.https.HttpsError(
        "internal",
        "No se pudo obtener la configuración segura.",
        error.message, // Incluye el mensaje original para logs
    );
  }
}
/**
 * Normaliza una cadena de ubicación para usarla como ID.
 * @param {string | undefined} locationString La cadena de ubicación.
 * @return {string | null} El ID normalizado o null si la entrada no es válida.
 */
function normalizeLocationId(locationString) {
  if (!locationString || typeof locationString !== "string") return null;
  return locationString
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, "*") // Reemplaza no alfanuméricos con *
      .replace(/\*+/g, "*") // Colapsa múltiples * a uno solo
      .replace(/^\*+|\*+$/g, ""); // Quita * al inicio/final
}

// --- Cloud Function Principal (HTTPS Callable) ---
exports.obtenerCultivosPorUbicacion = functions.https.onCall(
    async (data, context) => {
      const ubicacionOriginal = data.ubicacion;

      // 1. Validar Input
      if (!ubicacionOriginal || typeof ubicacionOriginal !== "string") {
        console.error("Input inválido:", data);
        throw new functions.https.HttpsError(
            "invalid-argument",
            "La función debe llamarse con un argumento \"ubicacion\" " +
            "de tipo string.",
        );
      }
      // eslint-disable-next-line max-len
      console.log(`Recibida solicitud para ubicación: "${ubicacionOriginal}"`);

      // 2. Normalizar ID
      const idNormalizado = normalizeLocationId(ubicacionOriginal);
      if (!idNormalizado) {
        // eslint-disable-next-line max-len
        console.error(`No se pudo normalizar la ubicación: "${ubicacionOriginal}"`);
        throw new functions.https.HttpsError(
            "invalid-argument",
            "Formato de ubicación no válido.",
        );
      }
      console.log(`ID Normalizado: ${idNormalizado}`);

      // 3. Consultar Caché (Firestore)
      // eslint-disable-next-line max-len
      const docRef = db.collection("ubicacionesCultivos").doc(idNormalizado);
      try {
        const docSnap = await docRef.get();

        if (docSnap.exists) {
          console.log(`Cache hit para: ${idNormalizado}`);
          const datosExistentes = docSnap.data();
          // Asegurarse que el campo 'cultivos' existe y es un array
          if (datosExistentes && Array.isArray(datosExistentes.cultivos)) {
            // eslint-disable-next-line max-len
            console.log(`Devolviendo ${datosExistentes.cultivos.length} cultivos desde caché.`);
            return {cultivos: datosExistentes.cultivos};
          } else {
            console.warn(
                `Documento ${idNormalizado} existe pero sin campo ` +
                "'cultivos' válido. Consultando OpenAI.",
            );
          }
        } else {
          // eslint-disable-next-line max-len
          console.log(`Cache miss para: ${idNormalizado}. Consultando OpenAI...`);

          // 4. Si no existe (Cache Miss), llamar a OpenAI
          // Obtenemos la API Key (el try/catch inútil fue removido)
          // El catch general de la función manejará los errores de aquí.
          const apiKey = await getOpenAiApiKey();

          // 4a. Preparar Prompt para OpenAI (¡Pidiendo JSON!)
          // Prompt dividido en líneas para cumplir max-len
          const prompt = `
Eres un asistente experto en agricultura y botánica.
Para la ubicación geográfica "${ubicacionOriginal}", lista los 5-7
cultivos agrícolas más comunes y relevantes.
Para cada cultivo, proporciona su nombre común y una breve
descripción (1-2 frases enfocadas en su relevancia o
características principales en esa zona).
IMPORTANTE: Formatea tu respuesta EXCLUSIVAMENTE como un array JSON
válido. Cada elemento del array debe ser un objeto con las claves
"nombre" (string) y "descripcion" (string).
Ejemplo de formato esperado:
[
  {"nombre": "Cultivo A", "descripcion": "Descripción breve de A."},
  {"nombre": "Cultivo B", "descripcion": "Descripción breve de B."}
]
Si no encuentras información específica o la ubicación no es válida
para agricultura, devuelve un array JSON vacío: [].
`.trim();

          const openAiUrl = "https://api.openai.com/v1/chat/completions";
          const headers = {
            "Authorization": `Bearer ${apiKey}`,
            "Content-Type": "application/json",
          };
          const bodyJson = {
            // Modelo - comentario dividido
            model: "gpt-4o-mini", // O "gpt-3.5-turbo" si prefieres
            // (más rápido, menos capaz)
            messages: [
              // {role: "system", content: "Eres un asistente agrícola."},
              {role: "user", content: prompt},
            ],
            temperature: 0.5, // Un poco menos creativo para datos
            max_tokens: 400, // Ajusta según necesidad
            // response_format - comentario dividido
            // response_format: { type: "json_object" }
            // Descomenta si usas GPT-4 Turbo o posterior que lo soporte bien
          };

          // 4b. Realizar la llamada a OpenAI
          let openAiResponseData;
          try {
            const response = await axios.post(openAiUrl, bodyJson, {headers});
            openAiResponseData = response.data;
          } catch (apiError) {
            // Mensaje de error dividido
            const errorDetails = apiError.response?.data || apiError.message;
            console.error("Error en API OpenAI:", errorDetails);
            throw new functions.https.HttpsError(
                "unavailable", // O 'internal'
                "Error al contactar al servicio de IA.",
                apiError.message,
            );
          }

          // 4c. Procesar Respuesta de OpenAI
          let cultivosParseados = [];
          // eslint-disable-next-line max-len
          if (openAiResponseData && openAiResponseData.choices && openAiResponseData.choices.length > 0) {
            // eslint-disable-next-line max-len
            const content = openAiResponseData.choices[0].message.content.trim();
            console.log("Respuesta cruda de OpenAI:", content);
            try {
              cultivosParseados = JSON.parse(content);
              if (!Array.isArray(cultivosParseados)) {
                // eslint-disable-next-line max-len
                console.error("OpenAI no devolvió un array JSON válido:", content);
                cultivosParseados = [];
              }
              // Filtro dividido en líneas
              cultivosParseados = cultivosParseados.filter((item) =>
                item &&
                typeof item.nombre === "string" &&
                typeof item.descripcion === "string",
              );
            } catch (parseError) {
              // Mensaje de error dividido
              // eslint-disable-next-line max-len
              console.error("Error parseando JSON de OpenAI:", parseError);
              console.error("Contenido recibido:", content);
              cultivosParseados = [];
            }
          } else {
            // eslint-disable-next-line max-len
            console.error("Respuesta inesperada o vacía de OpenAI:", openAiResponseData);
          }

          // 5. Guardar en Firestore (Actualizar Caché)
          if (cultivosParseados.length > 0) {
            const datosParaGuardar = {
              nombreOriginal: ubicacionOriginal,
              cultivos: cultivosParseados,
              fechaConsulta: admin.firestore.FieldValue.serverTimestamp(),
            };
            try {
              await docRef.set(datosParaGuardar);
              // eslint-disable-next-line max-len
              console.log(`Datos guardados en caché para: ${idNormalizado}`);
            } catch (dbError) {
              // eslint-disable-next-line max-len
              console.error(`Error guardando en Firestore para ${idNormalizado}:`, dbError);
            }
          } else {
            // Mensaje de log dividido
            console.log(
                `No se obtuvieron datos válidos de OpenAI para ` +
                `${idNormalizado}, no se guarda en caché.`,
            );
            // Comentario largo dividido
            // Considera guardar un registro vacío o con error
            // si quieres evitar reintentos constantes
            // await docRef.set({
            //   nombreOriginal: ubicacionOriginal,
            //   cultivos: [],
            //   fechaConsulta: admin.firestore.FieldValue.serverTimestamp(),
            //   error: "No data from AI"
            // });
          }

          // 6. Devolver Respuesta a la App
          // eslint-disable-next-line max-len
          console.log(`Devolviendo ${cultivosParseados.length} cultivos obtenidos de OpenAI.`);
          return {cultivos: cultivosParseados};
        } // Fin del bloque else (cache miss)
      } catch (error) {
        // Mensaje de error dividido
        console.error(
            `Error procesando ubicación ${ubicacionOriginal} ` +
            `(ID: ${idNormalizado}):`,
            error,
        );
        // Asegúrate de que el error lanzado sea HttpsError si no lo es ya
        if (error instanceof functions.https.HttpsError) {
          throw error;
        } else {
          // Mensaje de error dividido
          throw new functions.https.HttpsError(
              "internal",
              "Ocurrió un error inesperado procesando la solicitud.",
              error.message,
          );
        }
      }
    });
