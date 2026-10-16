import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.springframework.security.crypto.bcrypt.BCrypt;

/** Helper local: contraseña por stdin, hash por stdout; nunca recibe claves en argumentos. */
class HashLocalPassword {
    public static void main(String[] args) throws Exception {
        String password = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)).readLine();
        int minimum = args.length == 1 && args[0].equals("--local-min8") ? 8 : 12;
        if (password == null || password.length() < minimum || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("La clave local debe tener al menos " + minimum + " caracteres y máximo 72 bytes.");
        }
        System.out.print(BCrypt.hashpw(password, BCrypt.gensalt(12)));
    }
}
