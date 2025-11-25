const admin = require('firebase-admin');
// 같은 config 폴더 안에 있으므로 ./ 사용
const serviceAccount = require('./serviceAccountKey.json'); 

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

module.exports = admin;