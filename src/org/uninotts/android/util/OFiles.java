package org.uninotts.android.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import android.content.Context;

public class OFiles {

	public static String getFolder(Context context) {
		return context.getFilesDir() + File.separator;
	}

	public static boolean saveObject(Object o, File file) throws IOException {
		FileOutputStream fos = new FileOutputStream(file);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(o);
		oos.close();
		fos.close();
		return true;
	}

	public static boolean saveObject(Object o, String file) throws IOException {
		FileOutputStream fos = new FileOutputStream(file);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(o);
		oos.close();
		fos.close();
		return true;
	}

	public static Object read(String file) throws IOException,
			ClassNotFoundException {
		Object o = null;
		FileInputStream fileIn = new FileInputStream(file);
		ObjectInputStream in = new ObjectInputStream(fileIn);
		o = in.readObject();
		in.close();
		fileIn.close();
		return o;
	}

}
