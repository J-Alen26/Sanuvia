const functions = require("firebase-functions");
const admin = require("firebase-admin");
const axios = require("axios");
const { SecretManagerServiceClient } = require("@google-cloud/secret-manager");

// Inicializa Firebase Admin SDK (si no lo has hecho)
// admin.initializeApp(); // Descomenta si es necesario inicializar aquí

// Inicializa el cliente de Secret Manager
const secretManagerClient = new SecretManagerServiceClient();

// Función para obtener el secreto
async function getOpenAiApiKey() {
  // Reemplaza con tu Project ID y el nombre/versión del secreto
  const name = "projects/anfeca2025/secrets/OPENAI_API_KEY/versions/latest";
  try {
    const [version] = await secretManagerClient.accessSecretVersion({ name });
    const payload = version.payload.data.toString("utf8");
    return payload;
  } catch (error) {
    console.error("Error accessing secret manager:", error);
    throw new functions.https.HttpsError(
      "internal",
      "Could not retrieve API Key.",
      error,
    );
  }
}

// --- Tu Cloud Function Callable ---
exports.analizarImagenOpenAI = functions.https.onCall(async (data, context) => {
  // Opcional: Verificar si el usuario está autenticado
  // if (!context.auth) {
  //   throw new functions.https.HttpsError(
  //       'unauthenticated',
  //       'The function must be called while authenticated.'
  //   );
  // }

  const imageUrl = data.imageUrl;
  if (!imageUrl || typeof imageUrl !== "string") {
    throw new functions.https.HttpsError(
      "invalid-argument",
      'The function must be called with a valid "imageUrl" argument.',
    );
  }

  console.log(`Analizando URL: ${imageUrl}`);

  let apiKey;
  try {
    apiKey = await getOpenAiApiKey();
  } catch (error) {
    // El error ya fue logueado en getOpenAiApiKey
    // Lanzamos el error para que el cliente lo reciba
    throw error;
  }

  // Construir el payload para OpenAI (similar a tu código Kotlin)
  const systemContent = `
        Eres un especialista en nutrición y seguridad alimentaria.
        Analiza la imagen enviada y devuélveme:
        - Descripción nutricional (macro y micronutrientes más relevantes)
        - Calorías aproximadas por porción
        - Recomendaciones de consumo o alertas (alergias, exceso de azúcares, etc.)
        Responde en un párrafo claro para el usuario final.
    `.trim();

  const messages = [
    { role: "system", content: systemContent },
    {
      role: "user",
      content: [
        {
          type: "image_url",
          image_url: { url: imageUrl },
        },
      ],
    },
  ];

  const bodyJson = {
    model: "gpt-4o-mini", // O el modelo que prefieras
    messages: messages,
    temperature: 0.6,
    max_tokens: 300,
  };

  const openAiUrl = "https://api.openai.com/v1/chat/completions";
  const headers = {
    Authorization: `Bearer ${apiKey}`,
    "Content-Type": "application/json",
  };

  // Realizar la llamada a OpenAI
  try {
    console.log("Llamando a OpenAI...");
    const response = await axios.post(openAiUrl, bodyJson, { headers });

    if (response.data && response.data.choices && response.data.choices.length > 0) {
      const description = response.data.choices[0].message.content.trim();
      console.log("Descripción obtenida:", description);
      // Devuelve solo la descripción
      return { description: description };
    } else {
      console.error("Respuesta inesperada de OpenAI:", response.data);
      throw new functions.https.HttpsError(
        "internal",
        "Respuesta inesperada de OpenAI.",
      );
    }
  } catch (error) {
// ---- PRUEBA TEMPORAL ----
console.error("Error llamando a OpenAI (simple):", error.message);
// ---- FIN PRUEBA ----    // Propaga un error genérico o más específico si es posible
    throw new functions.https.HttpsError(
      "internal",
      "Error al procesar la imagen con IA.",
      error.message,
    );
  }
});