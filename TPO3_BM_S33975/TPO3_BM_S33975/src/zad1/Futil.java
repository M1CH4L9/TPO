package zad1;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;

public class Futil {

  public static void processDir(String dirName, String resultFileName) {
    Charset inCharset = Charset.forName("Cp1250");
    Charset outCharset = StandardCharsets.UTF_8;
    Path start = Path.of(dirName);
    Path result = Path.of(resultFileName);

    try (FileChannel out = FileChannel.open(
        result,
        StandardOpenOption.CREATE,
        StandardOpenOption.WRITE,
        StandardOpenOption.TRUNCATE_EXISTING)) {

      Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
          try (FileChannel in = FileChannel.open(file, StandardOpenOption.READ)) {
            ByteBuffer inBuffer = ByteBuffer.allocate((int) in.size());
            while (inBuffer.hasRemaining()) {
              if (in.read(inBuffer) < 0) {
                break;
              }
            }
            inBuffer.flip();

            CharBuffer chars = inCharset.decode(inBuffer);
            ByteBuffer outBuffer = outCharset.encode(chars);
            while (outBuffer.hasRemaining()) {
              out.write(outBuffer);
            }
          } catch (IOException e) {
          }
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
          return FileVisitResult.CONTINUE;
        }
      });
    } catch (Exception e) {
    }
  }
}
