# Securepool
#### Georgia Southwestern State University  
#### CSCI 6130 - Mobile Security  
#### Summer 2025  
  
Purpose: To design an insecure Android application, identify and model threats, and then secure the app by applying techniques learned from CSCI 6130 - Mobile Security.  
  
## Prerequisites  
- Android Studio  
- MySQL  
- NodeJS  
- OpenSSL  

## Backend Server  
- From the `backend` directory, run `npm install`  
- Update the `connectionProperties` object in `initializeDatabase.js` to the correct port, user, and password for your MySQL installation (insecure hard coded credentials will be patched in v2).  
- Make a directory to hold a development https certificate `mkdir .cert`  
- `cd .cert`  
- Generate a private key and certificate for use in development  
`openssl req -x509 -newkey rsa:4096 -keyout securepool_key.pem -out securepool_cert.pem -sha256 -days 365 -nodes -subj "/C=US/ST=California/L=Mountain View/O=AndroidDev/CN=localhost" -addext "subjectAltName = DNS:localhost,IP:10.0.2.2"`  
- Run `npm run start`  

## Android Application
- Copy `securepool_cert.pem` generated in the Backend Server setup to your emulated Android device in Android Studio by dragging it to the virtual device's screen.  
- Install the certificate on the Android device `Settings > Security > Encryption & credentials > Install a certificate > CA Certificate` and locate `securepool_cert.pem`  