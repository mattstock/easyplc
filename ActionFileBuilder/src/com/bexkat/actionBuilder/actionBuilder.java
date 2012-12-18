package com.bexkat.actionBuilder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class actionBuilder {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		BufferedWriter bw;
		BufferedReader br;
		String line;
		byte msg;
		try {
			br = new BufferedReader(new FileReader("c:\\users\\stock\\desktop\\command.txt"));
			bw = new BufferedWriter(new FileWriter("c:\\users\\stock\\desktop\\active.txt"));
			while ((line = br.readLine()) != null) {
				String tok[] = line.split(":");
				switch (tok[0].charAt(0)) {
				case 'x':
					msg = (byte) (0x3f & Integer.parseInt(tok[1]));
					break;
				case 'y':
					msg = (byte) ( 0x40 | (0x3f & Integer.parseInt(tok[1])));
					break;					
				case 'z':
					msg = (byte) ( 0x80 | (0x3f & Integer.parseInt(tok[1])));
					break;
				case '0':
					msg = (byte) ( 0xc0 | (0x3f & Integer.parseInt(tok[1])));
					break;
				default:
					msg = (byte) ( 0xd0 | (0x3f & Integer.parseInt(tok[1])));
					break;
				}
				System.out.println(String.format("write %2x", msg));
				bw.write(msg);
			}
			br.close();
			bw.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
