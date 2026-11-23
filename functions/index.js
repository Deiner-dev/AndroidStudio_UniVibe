const functions = require("firebase-functions");
const admin = require("firebase-admin");
const { S3Client, PutObjectCommand, GetObjectCommand } = require("@aws-sdk/client-s3");
const { getSignedUrl } = require("@aws-sdk/s3-request-presigner");
const { AccessToken } = require("livekit-server-sdk");

admin.initializeApp();

const LIVEKIT_API_KEY = process.env.LIVEKIT_API_KEY || "APIMzjFh58Lfsfm";
const LIVEKIT_API_SECRET = process.env.LIVEKIT_API_SECRET || "eCGAudrCkMa1eMrGex7zbZBd0b3lvAh8uOl6InQnTHvA";
const LIVEKIT_WS_URL = process.env.LIVEKIT_WS_URL || "wss://univibe-oy1p5z3k.livekit.cloud";

// Configuración de Backblaze B2 (Variables de entorno / Secretos)
const B2_BUCKET_NAME = process.env.B2_BUCKET_NAME || "univibe-media";
const B2_BUCKET_ID = process.env.B2_BUCKET_ID || "cb68e51028fbc7c5ae100a17";
const B2_REGION = process.env.B2_REGION || "us-east-005";
const B2_ENDPOINT = process.env.B2_ENDPOINT || "https://s3.us-east-005.backblazeb2.com";
const B2_KEY_ID = process.env.B2_KEY_ID;
const B2_APPLICATION_KEY = process.env.B2_APPLICATION_KEY;

// Cliente S3 para Backblaze B2
function getS3Client() {
  return new S3Client({
    region: B2_REGION,
    endpoint: B2_ENDPOINT,
    credentials: {
      accessKeyId: B2_KEY_ID || "DUMMY_KEY_ID",
      secretAccessKey: B2_APPLICATION_KEY || "DUMMY_APPLICATION_KEY"
    }
  });
}

/**
 * Genera una URL firmada de subida (Presigned Upload URL) en Backblaze B2.
 * Asocia obligatoriamente el archivo al UID del usuario autenticado en Firebase Auth.
 */
exports.generarUrlSubidaB2 = functions.https.onRequest(async (req, res) => {
  res.set("Access-Control-Allow-Origin", "*");
  if (req.method === "OPTIONS") {
    res.set("Access-Control-Allow-Methods", "POST");
    res.set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    return res.status(204).send("");
  }

  try {
    const authHeader = req.headers.authorization;
    if (!authHeader || !authHeader.startsWith("Bearer ")) {
      return res.status(401).json({ error: "No autorizado. Token de Firebase Auth requerido." });
    }

    const idToken = authHeader.split("Bearer ")[1];
    const decodedToken = await admin.auth().verifyIdToken(idToken);
    const uid = decodedToken.uid; // UID autenticado obligatoriamente

    const { fileName, fileType, mediaCategory } = req.body || {};
    if (!fileName || !fileType) {
      return res.status(400).json({ error: "Faltan parámetros: fileName o fileType" });
    }

    const category = mediaCategory || "videos";
    const objectKey = `${category}/${uid}/${Date.now()}_${fileName}`;

    const s3Client = getS3Client();
    const command = new PutObjectCommand({
      Bucket: B2_BUCKET_NAME,
      Key: objectKey,
      ContentType: fileType,
      Metadata: {
        ownerUid: uid
      }
    });

    // URL firmada válida por 15 minutos
    const uploadUrl = await getSignedUrl(s3Client, command, { expiresIn: 900 });

    return res.status(200).json({
      uploadUrl,
      objectKey,
      bucket: B2_BUCKET_NAME,
      uid
    });
  } catch (error) {
    console.error("Error al generar URL de subida B2:", error);
    return res.status(500).json({ error: error.message || "Error interno del servidor" });
  }
});

/**
 * Genera una URL de lectura firmada (Presigned Download URL) para reproducir videos o ver imágenes privadas de B2.
 */
exports.generarUrlLecturaB2 = functions.https.onRequest(async (req, res) => {
  res.set("Access-Control-Allow-Origin", "*");
  if (req.method === "OPTIONS") {
    res.set("Access-Control-Allow-Methods", "GET, POST");
    res.set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    return res.status(204).send("");
  }

  try {
    const objectKey = req.query.objectKey || (req.body && req.body.objectKey);
    if (!objectKey) {
      return res.status(400).json({ error: "Parámetro objectKey es requerido." });
    }

    const s3Client = getS3Client();
    const command = new GetObjectCommand({
      Bucket: B2_BUCKET_NAME,
      Key: objectKey
    });

    // URL firmada de lectura válida por 2 horas (7200 seg)
    const downloadUrl = await getSignedUrl(s3Client, command, { expiresIn: 7200 });

    return res.status(200).json({
      downloadUrl,
      objectKey
    });
  } catch (error) {
    console.error("Error al generar URL de lectura B2:", error);
    return res.status(500).json({ error: error.message || "Error interno del servidor" });
  }
});

/**
 * Genera un Access Token de LiveKit para unirse a una sala de voz o video.
 */
exports.generarLiveKitToken = functions.https.onRequest(async (req, res) => {
  res.set("Access-Control-Allow-Origin", "*");
  if (req.method === "OPTIONS") {
    res.set("Access-Control-Allow-Methods", "POST");
    res.set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    return res.status(204).send("");
  }

  try {
    const authHeader = req.headers.authorization;
    if (!authHeader || !authHeader.startsWith("Bearer ")) {
      return res.status(401).json({ error: "No autorizado." });
    }

    const idToken = authHeader.split("Bearer ")[1];
    const decodedToken = await admin.auth().verifyIdToken(idToken);
    const uid = decodedToken.uid;

    const { roomName, participantName } = req.body || {};
    if (!roomName) {
      return res.status(400).json({ error: "Falta el parámetro roomName" });
    }

    const at = new AccessToken(LIVEKIT_API_KEY, LIVEKIT_API_SECRET, {
      identity: uid,
      name: participantName || "Usuario UniVibe"
    });

    at.addGrant({ roomJoin: true, room: roomName, canPublish: true, canSubscribe: true });

    const token = await at.toJwt();

    return res.status(200).json({
      token,
      wsUrl: LIVEKIT_WS_URL
    });
  } catch (error) {
    console.error("Error al generar token de LiveKit:", error);
    return res.status(500).json({ error: error.message || "Error interno" });
  }
});
