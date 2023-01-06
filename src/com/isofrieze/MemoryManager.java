package com.isofrieze;
public class MemoryManager {
	
	private byte[] memory;
	
	public MemoryManager(int size) {
		this.memory = new byte[size];
	}
	
	public void write8(int value, int address) {
		this.memory[address] = (byte)value;
	}
	
	public int read8(int address) {
		return 0xFF & this.memory[address % this.memory.length];
	}
	
	public int read16(int address) {
		return read8(address) | (read8(address+1) << 8);
	}
	
	public int readBits(int address, int mask) {
		return getBits(read8(address), mask);
	}
	
	public int getBits(int value, int mask) {
		if (mask == 0) return 0;
		int b = value & mask;
		while ((mask & 1) == 0) {
			b >>= 1;
			mask >>= 1;
		}
		return b;
	}
	
}