const fs=require('fs');
const lines=fs.readFileSync('src/main/java/com/cloudkitchen/rbac/service/impl/AuthServiceImpl.java','utf16le').split('\\r');
for (let i=470; i<590; i++){console.log((i+1)+':' + lines[i]);}
console.log('len', lines.length);
