package uk.ac.lancs.ucrel.file.system;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.CharBuffer;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class FileUtils {

    public static FileChannel getNewFileChannel(Path file, String mode) throws IOException {
        Files.createFile(file);
        return getFileChannel(file, mode);
    }

    public static FileChannel getFileChannel(Path file, String mode) throws FileNotFoundException {
        RandomAccessFile f = new RandomAccessFile(file.toString(), mode);
        return f.getChannel();
    }

    public static MappedByteBuffer read(Path file, int pos, int length) throws IOException {
        return getFileChannel(file, "r").map(FileChannel.MapMode.READ_ONLY, pos, length);
    }

    public static MappedByteBuffer readAll(Path file) throws IOException {
        FileChannel fc = getFileChannel(file, "r");
        return fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
    }

    public static IntBuffer readInts(Path file, int pos, int length) throws IOException {
        return read(file, pos * 4, length * 4).asIntBuffer();
    }

    public static IntBuffer readAllInts(Path file) throws IOException {
        return readAll(file).asIntBuffer();
    }

    public static void write(Path file, byte[] data) throws IOException {
        FileChannel fc = getNewFileChannel(file, "rw");
        MappedByteBuffer mbb = fc.map(FileChannel.MapMode.READ_WRITE, 0, data.length);
        mbb.put(data);
        fc.close();
    }

    public static void write(Path file, CharBuffer data, Charset cs) throws IOException {
        FileChannel fc = getNewFileChannel(file, "rw");
        MappedByteBuffer mbb = fc.map(FileChannel.MapMode.READ_WRITE, 0, data.length());
        mbb.put(cs.encode(data));
        fc.close();
    }

    public static void write(Path file, List<String> data, Charset cs) throws IOException {
        FileChannel fc = getNewFileChannel(file, "rw");
        StringBuilder sb = new StringBuilder();
        int size = 0;
        for (String s : data) {
            size += s.length() + 1;
        }
        CharBuffer cb = CharBuffer.allocate(size);
        for (String s : data) {
            cb.append(s);
            cb.append('\n');
        }
        MappedByteBuffer mbb = fc.map(FileChannel.MapMode.READ_WRITE, 0, cb.length());
        mbb.put(cs.encode(cb));
        fc.close();
    }

    public static void write(Path file, int[] data) throws IOException {
        FileChannel fc = getNewFileChannel(file, "rw");
        IntBuffer ib = fc.map(FileChannel.MapMode.READ_WRITE, 0, data.length * 4).asIntBuffer();
        for (int i : data) {
            ib.put(i);
        }
        fc.close();
    }
}
