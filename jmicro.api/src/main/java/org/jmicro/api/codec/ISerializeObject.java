package org.jmicro.api.codec;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public abstract interface ISerializeObject {

	public abstract void encode(DataOutput buffer)  throws IOException;
	
	public abstract  void decode(DataInput buffer)  throws IOException;
	
}
