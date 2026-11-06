import com.dspro.android.data.notes.BackupZipCodec;
import java.io.*;
import java.nio.file.*;
import java.util.*;
public class BackupZipCodecHarness {
  public static void main(String[] args) throws Exception {
    if (args[0].equals("read")) { BackupZipCodec.read(Files.newInputStream(Path.of(args[1]))); return; }
    File dir = Files.createTempDirectory("codec-roundtrip").toFile();
    File attachment = new File(dir, "a.bin"); Files.write(attachment.toPath(), new byte[]{1,2,3,4});
    File archive = new File(dir, "valid.zip");
    Map<String,File> files = new LinkedHashMap<>(); files.put("files/f000000.bin", attachment);
    BackupZipCodec.write(new FileOutputStream(archive), "{\"format\":\"dspro-user-backup\"}".getBytes("UTF-8"), files);
    Map<String,byte[]> parsed = BackupZipCodec.read(new FileInputStream(archive));
    if (!Arrays.equals(parsed.get("files/f000000.bin"), new byte[]{1,2,3,4})) throw new AssertionError("attachment mismatch");
    if (!BackupZipCodec.validName("manifest.json") || BackupZipCodec.validName("../manifest.json") || BackupZipCodec.validName("/manifest.json") || BackupZipCodec.validName("files/f000000.bin/../../x")) throw new AssertionError("path rule mismatch");
    System.out.println("PASS: actual BackupZipCodec write/read roundtrip and path checks");
  }
}
