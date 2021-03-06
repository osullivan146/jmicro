package cn.jmicro.choreography.api;

import cn.jmicro.api.annotation.SO;

@SO
public class PackageResource {

	private String name;

	private long size;
	
	private int finishBlockNum = 0;
	
	private int totalBlockNum = 0;
	
	private int blockSize = 0;
	
	private String blockIndexFileName;
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public int getFinishBlockNum() {
		return finishBlockNum;
	}

	public void setFinishBlockNum(int finishBlockNum) {
		this.finishBlockNum = finishBlockNum;
	}

	public String getBlockIndexFileName() {
		return blockIndexFileName;
	}

	public void setBlockIndexFileName(String blockIndexFileName) {
		this.blockIndexFileName = blockIndexFileName;
	}

	public int getTotalBlockNum() {
		return totalBlockNum;
	}

	public void setTotalBlockNum(int totalBlockNum) {
		this.totalBlockNum = totalBlockNum;
	}

	public int getBlockSize() {
		return blockSize;
	}

	public void setBlockSize(int blockSize) {
		this.blockSize = blockSize;
	}

	@Override
	public int hashCode() {
		return name != null ? name.hashCode():0;
	}

	@Override
	public boolean equals(Object obj) {
		PackageResource pr = (PackageResource)obj;
		return this.name != null ? name.equals(pr.getName()):(pr.getName() == null);
	}

	@Override
	public String toString() {
		return "PackageResource [name=" + name + ", size=" + size + ", finishBlockNum=" + finishBlockNum
				+ ", totalBlockNum=" + totalBlockNum + ", blockSize=" + blockSize + ", blockIndexFileName="
				+ blockIndexFileName + "]";
	}
	
}
