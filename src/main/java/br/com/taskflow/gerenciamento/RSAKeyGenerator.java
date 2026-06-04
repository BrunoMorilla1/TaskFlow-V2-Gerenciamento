package br.com.taskflow.gerenciamento;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import java.io.FileWriter;
import java.io.IOException;

public class RSAKeyGenerator {

    public static void main(String[] args) throws Exception {
        // Gera par de chaves RSA de 2048 bits
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();

        PrivateKey privateKey = kp.getPrivate(); // PKCS#8
        PublicKey publicKey = kp.getPublic();    // X.509

        // Salva as chaves no formato PEM
        writeKeyToFile("src/main/resources/jwt/private.pem", privateKey.getEncoded(), "PRIVATE KEY");
        writeKeyToFile("src/main/resources/jwt/public.pem", publicKey.getEncoded(), "PUBLIC KEY");

        System.out.println("Chaves geradas com sucesso!");
    }

    private static void writeKeyToFile(String path, byte[] keyBytes, String keyType) throws IOException {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append("-----BEGIN ").append(keyType).append("-----\n");

        // Base64 MIME com linhas de 64 caracteres
        String encoded = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(keyBytes);
        keyBuilder.append(encoded);
        keyBuilder.append("\n-----END ").append(keyType).append("-----\n");

        try (FileWriter fw = new FileWriter(path)) {
            fw.write(keyBuilder.toString());
        }
    }
}