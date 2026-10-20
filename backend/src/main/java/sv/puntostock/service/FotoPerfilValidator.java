package sv.puntostock.service;

import org.springframework.web.server.ResponseStatusException;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

public final class FotoPerfilValidator {
 private FotoPerfilValidator(){}
 public static String validar(String imagen){
  if(imagen==null)throw invalida();
  if(imagen.isEmpty())return null;
  if(imagen.length()>1_500_000||!imagen.matches("^data:image/(jpeg|png);base64,[A-Za-z0-9+/=]+$"))throw invalida();
  try{
   byte[] bytes=Base64.getDecoder().decode(imagen.substring(imagen.indexOf(',')+1));
   if(bytes.length>1_048_576)throw invalida();
   try(var input=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))){
    var readers=ImageIO.getImageReaders(input);if(!readers.hasNext())throw invalida();
    var reader=readers.next();
    try{
     reader.setInput(input);String format=reader.getFormatName().toLowerCase(java.util.Locale.ROOT);
     if(!(format.equals("jpeg")&&imagen.startsWith("data:image/jpeg;"))&&
        !(format.equals("png")&&imagen.startsWith("data:image/png;")))throw invalida();
     int width=reader.getWidth(0),height=reader.getHeight(0);
     if(width<1||height<1||width>4096||height>4096||(long)width*height>4_000_000)throw invalida();
     reader.read(0); // Comprueba que el archivo completo se puede decodificar.
    }finally{reader.dispose();}
   }
   return imagen;
  }catch(Exception e){throw invalida();}
 }
 private static ResponseStatusException invalida(){return new ResponseStatusException(BAD_REQUEST,"Imagen inválida. Usa JPEG o PNG de hasta 1 MB y 4 megapíxeles.");}
}
