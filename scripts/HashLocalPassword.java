import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.springframework.security.crypto.bcrypt.BCrypt;

/** Helper local: contraseña por stdin, hash por stdout; nunca recibe claves en argumentos. */
class HashLocalPassword {
    public static void main(String[] args) throws Exception {
        String password = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)).readLine();
        if (password == null || password.length() < 12 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("La clave local debe tener al menos 12 caracteres y máximo 72 bytes.");
        }
        System.out.print(BCrypt.hashpw(password, BCrypt.gensalt(12)));
    }
}
