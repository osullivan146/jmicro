package org.jmicro.api.codec;

import java.io.DataInput;
import java.io.DataOutput;

public abstract interface SerializeObject {

	public abstract void encode(DataOutput buffer,Object obj);
	
	public abstract Object decode(DataInput buffer);
	
}
