@echo off
openssl pkcs8 -in platform.pk8 -inform DER -outform PEM -out platform.priv.pem -nocrypt
openssl pkcs12 -export -in platform.x509.pem -inkey platform.priv.pem -out platform.pk12 -password pass:weitai -name pda
keytool -importkeystore -deststorepass weitai -destkeypass weitai -destkeystore sign.keystore -srckeystore platform.pk12 -srcstoretype PKCS12 -srcstorepass weitai -alias pda