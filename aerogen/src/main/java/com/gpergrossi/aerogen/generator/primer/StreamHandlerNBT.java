package com.gpergrossi.aerogen.generator.primer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.gpergrossi.util.io.IStreamHandler;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;

public class StreamHandlerNBT implements IStreamHandler<NBTTagCompound> {

	public static StreamHandlerNBT INSTANCE = new StreamHandlerNBT();
	
	private StreamHandlerNBT() {}
	
	Writer<NBTTagCompound> WRITER = new Writer<NBTTagCompound>() {
		@Override
		public void write(OutputStream os, NBTTagCompound nbt) throws IOException {
			try (
				BufferedOutputStream bufferedStream = new BufferedOutputStream(os);
				DataOutputStream dataStream = new DataOutputStream(bufferedStream);
			) {
				CompressedStreamTools.write(nbt, dataStream);
			}
		}
	};
	
	Reader<NBTTagCompound> READER = new Reader<NBTTagCompound>() {
		@Override
		public NBTTagCompound read(InputStream is) throws IOException {
			BufferedInputStream bufferedStream = new BufferedInputStream(is);
			DataInputStream dataStream = new DataInputStream(bufferedStream);
			
			NBTTagCompound nbt = CompressedStreamTools.read(dataStream);
			return nbt;
		}
	};
	
	@Override
	public Writer<NBTTagCompound> getWriter() {
		return WRITER;
	}

	@Override
	public Reader<NBTTagCompound> getReader() {
		return READER;
	}

}
