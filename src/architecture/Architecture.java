package architecture;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;


import components.Bus;
import components.Demux;
import components.Memory;
import components.Register;
import components.Ula;

public class Architecture {
	
	private boolean simulation; //this boolean indicates if the execution is done in simulation mode.
								//simulation mode shows the components' status after each instruction
	private boolean halt;
    private int memorySize;
	private Bus extbus1;
	private Bus intbus1;
	private Bus intbus2;
	private Bus statusBits;
	private Memory memory;
	private Memory statusMemory;
	private Register PC;
	private Register IR;
	private Register RPG;
	private Register RPG1;
    private Register RPG2;
    private Register RPG3;
	private Register Flags;
    private Register stackTop;
    private Register stackBotton;
	private Ula ula;
	private Demux demux; //only for multiple register purposes
	
	private ArrayList<String> commandsList;
	private ArrayList<Register> registersList;
	
	

	/**
	 * Instanciates all components in this architecture
	 */
	private void componentsInstances() {
		//don't forget the instantiation order
		//buses -> registers -> ula -> memory
		extbus1 = new Bus();
		intbus1 = new Bus();
		intbus2 = new Bus();
		statusBits = new Bus();
		PC = new Register("PC", extbus1, intbus2);
		IR = new Register("IR", extbus1);
		RPG = new Register("RPG0", extbus1, intbus1);
		RPG1 = new Register ("RPG1", extbus1, intbus1);
		RPG2 = new Register ("RPG2", extbus1, intbus1);
		RPG3 = new Register ("RPG3", extbus1, intbus1);
		Flags = new Register (2, statusBits);
		stackBotton = new Register(20, intbus2);
		stackTop = new Register(20,intbus2);
		
		fillRegistersList();
		ula = new Ula(intbus1, intbus2);
		statusMemory = new Memory(2, extbus1);
		memorySize = 128;
		memory = new Memory(memorySize, extbus1);
		demux = new Demux(); //this bus is used only for multiple register operations
		
		fillCommandsList();
	}

	/**
	 * This method fills the registers list inserting into them all the registers we have.
	 * IMPORTANT!
	 * The first register to be inserted must be the default RPG
	 */
	private void fillRegistersList() {
		registersList = new ArrayList<Register>();
		registersList.add(RPG);
		registersList.add(RPG1);
		registersList.add(RPG2);
		registersList.add(RPG3);
		registersList.add(PC);
		registersList.add(IR);
		registersList.add(Flags);
		registersList.add(StackBotton);
		registersList.add(StackTop);
	}

	/**
	 * Constructor that instanciates all components according the architecture diagram
	 */
	public Architecture() {
		componentsInstances();
		
		//by default, the execution method is never simulation mode
		simulation = false;
	}

	
	public Architecture(boolean sim) {
		componentsInstances();
		
		//in this constructor we can set the simoualtion mode on or off
		simulation = sim;
	}



	//getters
	
	protected Bus getExtbus1() {
		return extbus1;
	}

	protected Bus getIntbus1() {
		return intbus1;
	}

	protected Bus getIntbus2() {
		return intbus2;
	}

	protected Memory getMemory() {
		return memory;
	}

	protected Register getPC() {
		return PC;
	}

	protected Register getIR() {
		return IR;
	}

	protected Register getRPG() {
		return RPG;
	}

	protected Register getFlags() {
		return Flags;
	}

	protected Ula getUla() {
		return ula;
	}

	public ArrayList<String> getCommandsList() {
		return commandsList;
	}



	//all the microprograms must be impemented here
	//the instructions table is
	/*
	 *
			add addr (rpg <- rpg + addr)
			sub addr (rpg <- rpg - addr)
			jmp addr (pc <- addr)
			jz addr  (se bitZero pc <- addr)
			jn addr  (se bitneg pc <- addr)
			read addr (rpg <- addr)
			store addr  (addr <- rpg)
			ldi x    (rpg <- x. x must be an integer)
			inc    (rpg++)
			move regA regB (regA <- regB)
	 */
	
	/**
	 * This method fills the commands list arraylist with all commands used in this architecture
	 */

	protected void fillCommandsList() {
		commandsList = new ArrayList<String>();
		commandsList.add("add"); //0
		commandsList.add("addRegReg"); //1
		commandsList.add("addMemReg"); //2
		commandsList.add("addRegMem"); //3
		commandsList.add("addImmReg"); //4
		commandsList.add("sub");   //5
		commandsList.add("subRegReg"); //6
		commandsList.add("subMemReg"); //7
		commandsList.add("subRegMem"); //8
		commandsList.add("subImmReg"); //9
		commandsList.add("read");  //13
		commandsList.add("store"); //14
		commandsList.add("ldi");   //15
		commandsList.add("moveMemReg");
		commandsList.add("moveRegMem");
		commandsList.add("moveRegReg"); 
		commandsList.add("moveImmReg"); 
		commandsList.add("inc");
		commandsList.add("jmp");  
		commandsList.add("jz");   
		commandsList.add("jn");   
	}

	protected void pcMaisMais(){
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore();
	}

	public void add() {
		pcMaisMais();
		 //now PC points to the parameter address
		RPG.internalRead();
		ula.store(0); //the rpg value is in ULA (0). This is the first parameter
		PC.read(); 
		memory.read(); // the parameter is now in the external bus. 
						//but the parameter is an address and we need the value
		memory.read(); //now the value is in the external bus
		RPG.store();
		RPG.internalRead();
		ula.store(1); //the rpg value is in ULA (0). This is the second parameter 
		ula.add(); //the result is in the second ula's internal register
		ula.internalRead(1);; //the operation result is in the internalbus 2
		setStatusFlags(intbus2.get()); //changing flags due the end of the operation
		RPG.internalStore(); //now the add is complete
		PC.internalRead(); //we need to make PC points to the next instruction address
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the next instruction. We go back to the FETCH status.
	}
	
	protected void addRegReg(){
		pcMaisMais();

		PC.read(); 
		memory.read(); // the second register
		demux.setValue(extbus1.get()); //points to the correct register
		registersInternalRead(); //starts the read from the register 
		ula.store(0);

		pcMaisMais();

		PC.read();
		memory.read(); // the second register
		demux.setValue(extbus1.get()); //points to the correct register
		registersInternalRead(); //starts the read from the register

		ula.store(1);
		ula.add();
		ula.internalRead(1);
		setStatusFlags(intbus2.get());
		ula.read(1);
		demux.setValue(extbus1.get()); //points to the correct register
		registersInternalStore();

		pcMaisMais();
	}
	protected void addMenReg(){
		pcMaisMais();

		PC.internalRead();
		StackBotton.store();
		


		PC.read(); 
		memory.read(); // the second register
		memory.read();
		PC.externalStore();
		PC.internalRead();
		ula.internalStore(0);
		StackBotton.read();
		PC.internalRead();
		pcMaisMais();
		
		PC.read();
		memory.read(); // the second register
		demux.setValue(extbus1.get()); //points to the correct register
		registersInternalRead(); //starts the read from the register

		ula.store(1);
		
		ula.add();
		ula.internalRead(1);
		setStatusFlags(intbus2.get());
		ula.read(1);
		demux.setValue(extbus1.get()); //points to the correct register
		registersInternalStore();

		pcMaisMais();
	}

	protected void addRegMen(){
		pcMaisMais();
		//Pegar o valor do registrador e guardar na ULA
		PC.read();
		memory.read(); 
		demux.setValue(extbus1.get()); 
		registersInternalRead(); 
		ula.store(0);
		pcMaisMais();
		//Guardar o valor do PC
		PC.internalRead();
		StackBotton.store();
		//Pegar o valor da memória, passar pelo PC, e meter na ULA
		PC.read();
		memory.read();
		memory.read();
		PC.store();
		PC.internalRead();
		ula.internalStore(1);
		//Fazer o add, e passar o valor da soma, pelo pc, e guardar no IR
		ula.add();
		ula.internalRead(1);
		setStatusFlags(intbus2.get());
		PC.internalStore();
		PC.read();
		IR.store();
		//Devolver o valor do PC, e pegar o endereço para armazenar o valor da soma
		StackBotton.read();
		PC.internalStore();
		PC.read();
		memory.read();
		memory.store();
		IR.read();
		memory.store();

		pcMaisMais();
	}

	protected void addImmReg(){
		pcMaisMais();

		//Guardar o valor do PC
		PC.internalRead();
		StackBotton.store();
		//Pegar o valor da memória, passar pelo PC, e meter na ULA
		PC.read();
		memory.read();
		PC.store();
		PC.internalRead();
		ula.internalStore(0);
		//Devolver o valor do PC e fazer o PC ++
		StackBotton.read();
		PC.internalStore();
		pcMaisMais();
		//Pegar o ID do registrador e jogar seu valor na ULA
		PC.read();
		memory.read();
		demux.setValue(extbus1.get()); //points to the correct register
		registersInternalRead(); //starts the read from the register
		ula.store(1);
		//Fazer o add, e passar o valor da soma para o registrador
		ula.add();
		ula.internalRead(1);
		setStatusFlags(intbus2.get());
		ula.read(1);
		demux.setValue(extbus1.get()); //points to the correct register
		registersInternalStore(); //starts the read from the register
		
		pcMaisMais();
	}


	/**
	 * This method implements the microprogram for
	 * 					SUB address
	 * In the machine language this command number is 1, and the address is in the position next to him
	 *    
	 * where address is a valid position in this memory architecture
	 * The method reads the value from memory (position address) and 
	 * performs an SUB with this value and that one stored in the rpg (the first register in the register list).
	 * The final result must be in RPG (the first register in the register list).
	 * The logic is
	 * 1. pc -> intbus2 //pc.read()
	 * 2. ula <-  intbus2 //ula.store()
	 * 3. ula incs
	 * 4. ula -> intbus2 //ula.read()
	 * 5. pc <- intbus2 //pc.store() now pc points to the parameter
	 * 6. rpg -> intbus1 //rpg.read() the current rpg value must go to the ula 
	 * 7. ula <- intbus1 //ula.store()
	 * 8. pc -> extbus (pc.read())
	 * 9. memory reads from extbus //this forces memory to write the data position in the extbus
	 * 10. memory reads from extbus //this forces memory to write the data value in the extbus
	 * 11. rpg <- extbus (rpg.store())
	 * 12. rpg -> intbus1 (rpg.read())
	 * 13. ula  <- intbus1 //ula.store()
	 * 14. Flags <- zero //the status flags are reset
	 * 15. ula subs
	 * 16. ula -> intbus1 //ula.read()
	 * 17. ChangeFlags //informations about flags are set according the result 
	 * 18. rpg <- intbus1 //rpg.store() - the add is complete.
	 * 19. pc -> intbus2 //pc.read() now pc must point the next instruction address
	 * 20. ula <- intbus2 //ula.store()
	 * 21. ula incs
	 * 22. ula -> intbus2 //ula.read()
	 * 23. pc <- intbus2 //pc.store() 
	 * end
	 * @param address
	 */
	public void sub() {
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the parameter address
		RPG.internalRead();
		ula.store(0); //the rpg value is in ULA (0). This is the first parameter
		PC.read(); 
		memory.read(); // the parameter is now in the external bus. 
						//but the parameter is an address and we need the value
		memory.read(); //now the value is in the external bus
		RPG.store();
		RPG.internalRead();
		ula.store(1); //the rpg value is in ULA (0). This is the second parameter
		ula.sub(); //the result is in the second ula's internal register
		ula.internalRead(1);; //the operation result is in the internalbus 2
		setStatusFlags(intbus2.get()); //changing flags due the end of the operation
		RPG.internalStore(); //now the sub is complete
		PC.internalRead(); //we need to make PC points to the next instruction address
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the next instruction. We go back to the FETCH status.
	}

	protected void subRegReg(){
		pcMaisMais();

		PC.read(); 
		memory.read(); // the second register
		demux.setValue(extbus1.get()); //points to the correct register
		registersInternalRead(); //starts the read from the register 
		ula.store(0);

		pcMaisMais();

		PC.read();
		memory.read(); // the second register
		demux.setValue(extbus1.get()); //points to the correct register
		registersInternalRead(); //starts the read from the register

		ula.store(1);
		ula.sub();
		ula.internalRead(1);
		setStatusFlags(intbus2.get());
		ula.read(1);
		demux.setValue(extbus1.get()); //points to the correct register
		registersInternalStore();

		pcMaisMais();
	}
	protected void subMemReg(){
		pcMaisMais();

		PC.internalRead();
		StackBotton.store();
		


		PC.read(); 
		memory.read(); // the second register
		memory.read();
		PC.externalStore();
		PC.internalRead();
		ula.internalStore(0);
		StackBotton.read();
		PC.internalRead();
		pcMaisMais();
		
		PC.read();
		memory.read(); // the second register
		demux.setValue(extbus1.get()); //points to the correct register
		registersInternalRead(); //starts the read from the register

		ula.store(1);
		
		ula.sub();
		ula.internalRead(1);
		setStatusFlags(intbus2.get());
		ula.read(1);
		demux.setValue(extbus1.get()); //points to the correct register
		registersInternalStore();

		pcMaisMais();
	}

	protected void subRegMem(){
		pcMaisMais();
		//Pegar o valor do registrador e guardar na ULA
		PC.read();
		memory.read(); 
		demux.setValue(extbus1.get()); 
		registersInternalRead(); 
		ula.store(0);
		pcMaisMais();
		//Guardar o valor do PC
		PC.internalRead();
		StackBotton.store();
		//Pegar o valor da memória, passar pelo PC, e meter na ULA
		PC.read();
		memory.read();
		memory.read();
		PC.store();
		PC.internalRead();
		ula.internalStore(1);
		//Fazer o sub, e passar o valor da soma, pelo pc, e guardar no IR
		ula.sub();
		ula.internalRead(1);
		setStatusFlags(intbus2.get());
		PC.internalStore();
		PC.read();
		IR.store();
		//Devolver o valor do PC, e pegar o endereço para armazenar o valor da soma
		StackBotton.read();
		PC.internalStore();
		PC.read();
		memory.read();
		memory.store();
		IR.read();
		memory.store();

		pcMaisMais();
	}

	protected void subImmReg(){
		pcMaisMais();

		//Guardar o valor do PC
		PC.internalRead();
		StackBotton.store();
		//Pegar o valor da memória, passar pelo PC, e meter na ULA
		PC.read();
		memory.read();
		PC.store();
		PC.internalRead();
		ula.internalStore(0);
		//Devolver o valor do PC e fazer o PC ++
		StackBotton.read();
		PC.internalStore();
		pcMaisMais();
		//Pegar o ID do registrador e jogar seu valor na ULA
		PC.read();
		memory.read();
		demux.setValue(extbus1.get()); //points to the correct register
		registersInternalRead(); //starts the read from the register
		ula.store(1);
		//Fazer o sub, e passar o valor da soma para o registrador
		ula.sub();
		ula.internalRead(1);
		setStatusFlags(intbus2.get());
		ula.read(1);
		demux.setValue(extbus1.get()); //points to the correct register
		registersInternalStore(); //starts the read from the register
		
		pcMaisMais();
	}




	public void jmp(){
		//PC++
		pcMaisMais();

		//Mem -> PC
		memory.read();
		PC.store();
	}

	public void jn(){
		//PC++
		pcMaisMais();
		if (Flags.getBit(1)==1){
			PC.read();
			memory.read();
			PC.store();
		}
		else{
			//PC++
			pcMaisMais();
		}
	}

	public void jz(){
		//PC++
		pcMaisMais();

		if (Flags.getBit(0)==1){
			PC.read();
			memory.read();
			PC.store();
		}
		else{
			//PC++
			pcMaisMais();
		}
	}
	
	public void jeq(){
		pcMaisMais();

		PC.read(); 
		memory.read(); // the second register
		demux.setValue(extbus1.get()); //points to the correct register
		registersInternalRead(); //starts the read from the register 
		ula.store(0);

		pcMaisMais();

		PC.read();
		memory.read(); // the second register
		demux.setValue(extbus1.get()); //points to the correct register
		registersInternalRead(); //starts the read from the register

		ula.store(1);
		ula.sub();
		ula.internalRead(1);
		setStatusFlags(intbus2.get());
		

		if (Flags.getBit(0)==1){
			pcMaisMais();

			PC.read();
			memory.read();
			PC.store();
		}
		else{
			pcMaisMais();

		}
	}
	public void jnz(){
		//PC++
		pcMaisMais();

		if (Flags.getBit(0)!=1){
			PC.read();
			memory.read();
			PC.store();
		}
		else{
			//PC++
			pcMaisMais();
		}
	}
	public void jnz(){
		//PC++
		pcMaisMais();

		if (Flags.getBit(0)!=1){
			PC.read();
			memory.read();
			PC.store();
		}
		else{
			//PC++
			pcMaisMais();
		}
	}

	public void jgt(){
		pcMaisMais();

		PC.read(); 
		memory.read(); // the second register
		demux.setValue(extbus1.get()); //points to the correct register
		registersInternalRead(); //starts the read from the register 
		ula.store(0);

		pcMaisMais();

		PC.read();
		memory.read(); // the second register
		demux.setValue(extbus1.get()); //points to the correct register
		registersInternalRead(); //starts the read from the register

		ula.store(1);
		ula.sub();
		ula.internalRead(1);
		setStatusFlags(intbus2.get());
		

		if (Flags.getBit(0)==0 && Flags.getBit(1)==0){
			pcMaisMais();

			PC.read();
			memory.read();
			PC.store();
		}
		else{
			pcMaisMais();

		}
	}

	public void jlw(){
		pcMaisMais();

		PC.read(); 
		memory.read(); // the second register
		demux.setValue(extbus1.get()); //points to the correct register
		registersInternalRead(); //starts the read from the register 
		ula.store(0);

		pcMaisMais();

		PC.read();
		memory.read(); // the second register
		demux.setValue(extbus1.get()); //points to the correct register
		registersInternalRead(); //starts the read from the register

		ula.store(1);
		ula.sub();
		ula.internalRead(1);
		setStatusFlags(intbus2.get());
		

		if (Flags.getBit(1)==1){
			pcMaisMais();
			PC.read();
			memory.read();
			PC.store();
		}
		else{
			pcMaisMais();

		}
	}

	/**
	 * This method is used after some ULA operations, setting the flags bits according the result.
	 * @param result is the result of the operation
	 * NOT TESTED!!!!!!!
	 */
	private void setStatusFlags(int result) {
		Flags.setBit(0, 0);
		Flags.setBit(1, 0);
		if (result==0) { //bit 0 in flags must be 1 in this case
			Flags.setBit(0,1);
		}
		if (result<0) { //bit 1 in flags must be 1 in this case
			Flags.setBit(1,1);
		}
	}

	/**
	 * This method implements the microprogram for
	 * 					ADD address
	 * In the machine language this command number is 0, and the address is in the position next to him
	 *    
	 * where address is a valid position in this memory architecture
	 * The method reads the value from memory (position address) and 
	 * performs an add with this value and that one stored in the RPG (the first register in the register list).
	 * The final result must be in RPG (the first register in the register list).
	 * The logic is
	 * 1. pc -> intbus2 //pc.read()
	 * 2. ula <-  intbus2 //ula.store()
	 * 3. ula incs
	 * 4. ula -> intbus2 //ula.read()
	 * 5. pc <- intbus2 //pc.store() now pc points to the parameter
	 * 6. rpg -> intbus1 //rpg.read() the current rpg value must go to the ula 
	 * 7. ula <- intbus1 //ula.store()
	 * 8. pc -> extbus (pc.read())
	 * 9. memory reads from extbus //this forces memory to write the data position in the extbus
	 * 10. memory reads from extbus //this forces memory to write the data value in the extbus
	 * 11. rpg <- extbus (rpg.store())
	 * 12. rpg -> intbus1 (rpg.read())
	 * 13. ula  <- intbus1 //ula.store()
	 * 14. Flags <- zero //the status flags are reset
	 * 15. ula adds
	 * 16. ula -> intbus1 //ula.read()
	 * 17. ChangeFlags //informations about flags are set according the result 
	 * 18. rpg <- intbus1 //rpg.store() - the add is complete.
	 * 19. pc -> intbus2 //pc.read() now pc must point the next instruction address
	 * 20. ula <- intbus2 //ula.store()
	 * 21. ula incs
	 * 22. ula -> intbus2 //ula.read()
	 * 23. pc <- intbus2 //pc.store() 
	 * end
	 * @param address
	 */
	

	
	
	/**
	 * This method implements the microprogram for
	 * 					JMP address
	 * In the machine language this command number is 2, and the address is in the position next to him
	 *    
	 * where address is a valid position in this memory architecture (where the PC is redirecto to)
	 * The method reads the value from memory (position address) and 
	 * inserts it into the PC register.
	 * So, the program is deviated
	 * The logic is
	 * 1. pc -> intbus2 //pc.read()
	 * 2. ula <-  intbus2 //ula.store()
	 * 3. ula incs
	 * 4. ula -> intbus2 //ula.read()
	 * 5. pc <- intbus2 //pc.store() now pc points to the parameter
	 * 6. pc -> extbus //pc.read()
	 * 7. memory reads from extbus //this forces memory to write the data position in the extbus
	 * 8. pc <- extbus //pc.store() //pc was pointing to another part of the memory
	 * end
	 * @param address
	 */

	/**
	 * This method implements the microprogram for
	 * 					JZ address
	 * In the machine language this command number is 3, and the address is in the position next to him
	 *    
	 * where address is a valid position in this memory architecture (where 
	 * the PC is redirected to, but only in the case the ZERO bit in Flags is 1)
	 * The method reads the value from memory (position address) and 
	 * inserts it into the PC register if the ZERO bit in Flags register is setted.
	 * So, the program is deviated conditionally
	 * The logic is
	 * 1. pc -> intbus2 //pc.read()
	 * 2. ula <-  intbus2 //ula.store()
	 * 3. ula incs
	 * 4. ula -> intbus2 //ula.read()
	 * 5. pc <- intbus2 //pc.internalstore() now pc points to the parameter
	 * 6. pc -> extbus1 //pc.read() now the parameter address is in the extbus1
	 * 7. Memory -> extbus1 //memory.read() the address (if jn) is in external bus 1
	 * 8. statusMemory(1)<- extbus1 // statusMemory.storeIn1()
	 * 9. ula incs
	 * 10. ula -> intbus2 //ula.read()
	 * 11. PC <- intbus2 // PC.internalStore() PC is now pointing to next instruction
	 * 12. PC -> extbus1 // PC.read() the next instruction address is in the extbus
	 * 13. statusMemory(0)<- extbus1 // statusMemory.storeIn0()
	 * 14. Flags(bitZero) -> extbus1 //the ZERO bit is in the external bus
	 * 15. statusMemory <- extbus // the status memory returns the correct address according the ZERO bit
	 * 16. PC <- extbus1 // PC stores the new address where the program is redirected to
	 * end
	 * @param address
	 */
	/*public void jz() {
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore();//now PC points to the parameter address
		PC.read();
		memory.read();// now the parameter value (address of the jz) is in the external bus
		statusMemory.storeIn1(); //the address is in position 1 of the status memory
		ula.inc();
		ula.internalRead(1);
		PC.internalStore();//now PC points to the next instruction
		PC.read();//now the bus has the next istruction address
		statusMemory.storeIn0(); //the address is in the position 0 of the status memory
		extbus1.put(Flags.getBit(0)); //the ZERO bit is in the external bus 
		statusMemory.read(); //gets the correct address (next instruction or parameter address)
		PC.store(); //stores into PC
	}*/
	
	/**
	 * This method implements the microprogram for
	 * 					jn address
	 * In the machine language this command number is 4, and the address is in the position next to him
	 *    
	 * where address is a valid position in this memory architecture (where 
	 * the PC is redirected to, but only in the case the NEGATIVE bit in Flags is 1)
	 * The method reads the value from memory (position address) and 
	 * inserts it into the PC register if the NEG bit in Flags register is setted.
	 * So, the program is deviated conditionally
	 * The logic is
	 * 1. pc -> intbus2 //pc.read()
	 * 2. ula <-  intbus2 //ula.store()
	 * 3. ula incs
	 * 4. ula -> intbus2 //ula.read()
	 * 5. pc <- intbus2 //pc.internalstore() now pc points to the parameter
	 * 6. pc -> extbus1 //pc.read() now the parameter address is in the extbus1
	 * 7. Memory -> extbus1 //memory.read() the address (if jn) is in external bus 1
	 * 8. statusMemory(1)<- extbus1 // statusMemory.storeIn1()
	 * 9. ula incs
	 * 10. ula -> intbus2 //ula.read()
	 * 11. PC <- intbus2 // PC.internalStore() PC is now pointing to next instruction
	 * 12. PC -> extbus1 // PC.read() the next instruction address is in the extbus
	 * 13. statusMemory(0)<- extbus1 // statusMemory.storeIn0()
	 * 14. Flags(bitNEGATIVE) -> extbus1 //the NEGATIVE bit is in the external bus
	 * 15. statusMemory <- extbus // the status memory returns the correct address according the ZERO bit
	 * 16. PC <- extbus1 // PC stores the new address where the program is redirected to
	 * end
	 * @param address
	 */
	/*public void jn() {
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore();//now PC points to the parameter address
		PC.read();
		memory.read();// now the parameter value (address of the jz) is in the external bus
		statusMemory.storeIn1(); //the address is in position 1 of the status memory
		ula.inc();
		ula.internalRead(1);
		PC.internalStore();//now PC points to the next instruction
		PC.read();//now the bus has the next istruction address
		statusMemory.storeIn0(); //the address is in the position 0 of the status memory
		extbus1.put(Flags.getBit(1)); //the ZERO bit is in the external bus 
		statusMemory.read(); //gets the correct address (next instruction or parameter address)
		PC.store(); //stores into PC
	}*/
	
	/**
	 * This method implements the microprogram for
	 * 					read address
	 * In the machine language this command number is 5, and the address is in the position next to him
	 *    
	 * where address is a valid position in this memory architecture 
	 * The method reads the value from memory (position address) and 
	 * inserts it into the RPG register (the first register in the register list)
	 * The logic is
	 * 1. pc -> intbus2 //pc.read()
	 * 2. ula <-  intbus2 //ula.store()
	 * 3. ula incs
	 * 4. ula -> intbus2 //ula.read()
	 * 5. pc <- intbus2 //pc.store() now pc points to the parameter
	 * 6. pc -> extbus //(pc.read())the address where is the position to be read is now in the external bus 
	 * 7. memory reads from extbus //this forces memory to write the address in the extbus
	 * 8. memory reads from extbus //this forces memory to write the stored data in the extbus
	 * 9. RPG <- extbus //the data is read
	 * 10. pc -> intbus2 //pc.read() now pc must point the next instruction address
	 * 11. ula <- intbus2 //ula.store()
	 * 12. ula incs
	 * 13. ula -> intbus2 //ula.read()
	 * 14. pc <- intbus2 //pc.store() 
	 * end
	 * @param address
	 */
	public void read() {
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the parameter address
		PC.read(); 
		memory.read(); // the address is now in the external bus.
		memory.read(); // the data is now in the external bus.
		RPG.store();
		PC.internalRead(); //we need to make PC points to the next instruction address
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the next instruction. We go back to the FETCH status.
	}
	
	/**
	 * This method implements the microprogram for
	 * 					store address
	 * In the machine language this command number is 6, and the address is in the position next to him
	 *    
	 * where address is a valid position in this memory architecture 
	 * The method reads the value from RPG (the first register in the register list) and 
	 * inserts it into the memory (position address) 
	 * The logic is
	 * 1. pc -> intbus2 //pc.read()
	 * 2. ula <-  intbus2 //ula.store()
	 * 3. ula incs
	 * 4. ula -> intbus2 //ula.read()
	 * 5. pc <- intbus2 //pc.store() now pc points to the parameter
	 * 6. pc -> extbus //(pc.read())the parameter address is the external bus
	 * 7. memory reads // memory reads the data in the parameter address. 
	 * 					// this data is the address where the RPG value must be stores 
	 * 8. memory stores //memory reads the address and wait for the value
	 * 9. RPG -> Externalbus //RPG.read()
	 * 10. memory stores //memory receives the value and stores it
	 * 11. pc -> intbus2 //pc.read() now pc must point the next instruction address
	 * 12. ula <- intbus2 //ula.store()
	 * 13. ula incs
	 * 14. ula -> intbus2 //ula.read()
	 * 15. pc <- intbus2 //pc.store() 
	 * end
	 * @param address
	 */
	public void store() {
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the parameter address
		PC.read(); 
		memory.read();   //the parameter address (pointing to the addres where data must be stored
		                 //is now in externalbus1
		memory.store(); //the address is in the memory. Now we must to send the data
		RPG.read();
		memory.store(); //the data is now stored
		PC.internalRead(); //we need to make PC points to the next instruction address
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the next instruction. We go back to the FETCH status.
	}
	
	/**
	 * This method implements the microprogram for
	 * 					ldi immediate
	 * In the machine language this command number is 7, and the immediate value
	 *        is in the position next to him
	 *    
	 * The method moves the value (parameter) into the internalbus1 and the RPG 
	 * (the first register in the register list) consumes it 
	 * The logic is
	 * 1. pc -> intbus2 //pc.read()
	 * 2. ula <-  intbus2 //ula.store()
	 * 3. ula incs
	 * 4. ula -> intbus2 //ula.read()
	 * 5. pc <- intbus2 //pc.store() now pc points to the parameter
	 * 6. pc -> extbus //(pc.read())the address where is the position to be read is now in the external bus 
	 * 7. memory reads from extbus //this forces memory to write the stored data in the extbus
	 * 8. RPG <- extbus //rpg.store()
	 * 9. 10. pc -> intbus2 //pc.read() now pc must point the next instruction address
	 * 10. ula <- intbus2 //ula.store()
	 * 11. ula incs
	 * 12. ula -> intbus2 //ula.read()
	 * 13. pc <- intbus2 //pc.store() 
	 * end
	 * @param address
	 */
	public void ldi() {
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the parameter address
		PC.read(); 
		memory.read(); // the immediate is now in the external bus.
		RPG.store();   //RPG receives the immediate
		PC.internalRead(); //we need to make PC points to the next instruction address
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the next instruction. We go back to the FETCH status.
	}
	
	/**
	 * This method implements the microprogram for
	 * 					inc 
	 * In the machine language this command number is 8
	 *    
	 * The method moves the value in rpg (the first register in the register list)
	 *  into the ula and performs an inc method
	 * 		-> inc works just like add rpg (the first register in the register list)
	 *         with the mumber 1 stored into the memory
	 * 		-> however, inc consumes lower amount of cycles  
	 * 
	 * The logic is
	 * 
	 * 1. rpg -> intbus1 //rpg.read()
	 * 2. ula  <- intbus1 //ula.store()
	 * 3. Flags <- zero //the status flags are reset
	 * 4. ula incs
	 * 5. ula -> intbus1 //ula.read()
	 * 6. ChangeFlags //informations about flags are set according the result
	 * 7. rpg <- intbus1 //rpg.store()
	 * 8. pc -> intbus2 //pc.read() now pc must point the next instruction address
	 * 9. ula <- intbus2 //ula.store()
	 * 10. ula incs
	 * 11. ula -> intbus2 //ula.read()
	 * 12. pc <- intbus2 //pc.store()
	 * end
	 * @param address
	 */
	public void inc() {
		pcMaisMais();
		PC.read();
		memory.read();
		demux.setValue(extbus1.get()); //points to the correct register
		registersInternalRead();
		ula.store(1);
		ula.inc();
		ula.read(1);
		setStatusFlags(intbus1.get());
		demux.setValue(extbus1.get()); //points to the correct register
		registersInternalStore();
		pcMaisMais();
	}
	public void call(){
		pcMaisMais();
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		stackTop.store();
		PC.read();
		memory.read();
		PC.store();
	}
	public void ret(){
		stackTop.read();
		PC.internalStore();
	}
	/**
	 * This method implements the microprogram for
	 * 					move <reg1> <reg2> 
	 * In the machine language this command number is 9
	 *    
	 * The method reads the two register ids (<reg1> and <reg2>) from the memory, in positions just after the command, and
	 * copies the value from the <reg1> register to the <reg2> register
	 * 
	 * 1. pc -> intbus2 //pc.read()
	 * 2. ula <-  intbus2 //ula.store()
	 * 3. ula incs
	 * 4. ula -> intbus2 //ula.read()
	 * 5. pc <- intbus2 //pc.store() now pc points to the first parameter
	 * 6. pc -> extbus //(pc.read())the address where is the position to be read is now in the external bus 
	 * 7. memory reads from extbus //this forces memory to write the parameter (first regID) in the extbus
	 * 8. pc -> intbus2 //pc.read() //getting the second parameter
	 * 9. ula <-  intbus2 //ula.store()
	 * 10. ula incs
	 * 11. ula -> intbus2 //ula.read()
	 * 12. pc <- intbus2 //pc.store() now pc points to the second parameter
	 * 13. demux <- extbus //now the register to be operated is selected
	 * 14. registers -> intbus1 //this performs the internal reading of the selected register 
	 * 15. PC -> extbus (pc.read())the address where is the position to be read is now in the external bus 
	 * 16. memory reads from extbus //this forces memory to write the parameter (second regID) in the extbus
	 * 17. demux <- extbus //now the register to be operated is selected
	 * 18. registers <- intbus1 //thid rerforms the external reading of the register identified in the extbus
	 * 19. 10. pc -> intbus2 //pc.read() now pc must point the next instruction address
	 * 20. ula <- intbus2 //ula.store()
	 * 21. ula incs
	 * 22. ula -> intbus2 //ula.read()
	 * 23. pc <- intbus2 //pc.store()  
	 * 		  
	 */


	public void moveMemReg() {

		pcMaisMais(); 
		//guardar o pc no Stack
		PC.internalRead();
		StackBotton.store();
		//pegar o valor da memoria e jogar na ula, atravessando o rio nilo e o PC
		PC.read(); 
		memory.read(); 
		memory.read();
		PC.externalStore();
		PC.internalRead();
		ula.internalStore(0);
		//resgatar o valor do PC
		StackBotton.read();
		PC.internalRead();
		pcMaisMais();
		//jogar o valor da memoria no registrador
		ula.read(0);
		PC.read();
		memory.read(); 
		demux.setValue(extbus1.get());
		registersInternalStore();
		pcMaisMais(); 
	}

	public void moveRegMem() {
		pcMaisMais();
		//pegar o id do registrador, e aguardar o comando
		PC.read();
		memory.read(); 
		demux.setValue(extbus1.get());
		pcMaisMais();
		//pegar o endereço de memoria, e dar o comando pro registrador cuspir seu dado, guardando na memoria
		PC.read();
		memory.read();
		memory.store();
		registersRead();
		memory.store();
		pcMaisMais(); 
	}
	public void moveRegReg() {
		pcMaisMais(); 
		PC.read();
		memory.read();
		demux.setValue(extbus1.get()); 
		registersInternalRead();
		pcMaisMais();
		PC.read();
		memory.read();
		demux.setValue(extbus1.get()); 
		registersInternalStore();
		pcMaisMais();
	}
	public void moveImmReg() {
		pcMaisMais(); 
		PC.read();
		memory.read();
		IR.store();
		pcMaisMais();
		PC.read();
		memory.read();
		demux.setValue(extbus1.get());
		IR.read();
		registersStore();
		pcMaisMais();
	}
	
	public ArrayList<Register> getRegistersList() {
		return registersList;
	}

	/**
	 * This method performs an (external) read from a register into the register list.
	 * The register id must be in the demux bus
	 */
	private void registersRead() {
		registersList.get(demux.getValue()).read();
	}
	
	/**
	 * This method performs an (internal) read from a register into the register list.
	 * The register id must be in the demux bus
	 */
	private void registersInternalRead() {
		registersList.get(demux.getValue()).internalRead();;
	}
	
	/**
	 * This method performs an (external) store toa register into the register list.
	 * The register id must be in the demux bus
	 */
	private void registersStore() {
		registersList.get(demux.getValue()).store();
	}
	
	/**
	 * This method performs an (internal) store toa register into the register list.
	 * The register id must be in the demux bus
	 */
	private void registersInternalStore() {
		registersList.get(demux.getValue()).internalStore();;
	}



	/**
	 * This method reads an entire file in machine code and
	 * stores it into the memory
	 * NOT TESTED
	 * @param filename
	 * @throws IOException 
	 */
	public void readExec(String filename) throws IOException {
		   BufferedReader br = new BufferedReader(new		 
		   FileReader(filename+".dxf"));
		   String linha;
		   int i=0;
		   while ((linha = br.readLine()) != null) {
			     extbus1.put(i);
			     memory.store();
			   	 extbus1.put(Integer.parseInt(linha));
			     memory.store();
			     i++;
			}
			br.close();
	}
	
	/**
	 * This method executes a program that is stored in the memory
	 */
	public void controlUnitEexec() {
		halt = false;
		while (!halt) {
			fetch();
			decodeExecute();
		}

	}
	

	/**
	 * This method implements The decode proccess,
	 * that is to find the correct operation do be executed
	 * according the command.
	 * And the execute proccess, that is the execution itself of the command
	 */
	private void decodeExecute() {
		IR.internalRead(); //the instruction is in the internalbus2
		int command = intbus2.get();
		simulationDecodeExecuteBefore(command);
		switch (command) {
		case 0:
			add();
			break;
		case 1:
			sub();
			break;
		case 2:
			jmp();
			break;
		case 3:
			jz();
			break;
		case 4:
			jn();
			break;
		case 5:
			read();
			break;
		case 6:
			store();
			break;
		case 7:
			ldi();
			break;
		case 8:
			inc();
			break;
		case 9:
			moveRegReg();
			break;
		default:
			halt = true;
			break;
		}
		if (simulation)
			simulationDecodeExecuteAfter();
	}

	/**
	 * This method is used to show the components status in simulation conditions
	 * NOT TESTED
	 * @param command 
	 */
	private void simulationDecodeExecuteBefore(int command) {
		System.out.println("----------BEFORE Decode and Execute phases--------------");
		String instruction;
		int parameter = 0;
		for (Register r:registersList) {
			System.out.println(r.getRegisterName()+": "+r.getData());
		}
		if (command !=-1)
			instruction = commandsList.get(command);
		else
			instruction = "END";
		if (hasOperands(instruction)) {
			parameter = memory.getDataList()[PC.getData()+1];
			System.out.println("Instruction: "+instruction+" "+parameter);
		}
		else
			System.out.println("Instruction: "+instruction);
		if ("read".equals(instruction))
			System.out.println("memory["+parameter+"]="+memory.getDataList()[parameter]);
		
	}

	/**
	 * This method is used to show the components status in simulation conditions
	 * NOT TESTED 
	 */
	private void simulationDecodeExecuteAfter() {
		String instruction;
		System.out.println("-----------AFTER Decode and Execute phases--------------");
		System.out.println("Internal Bus 1: "+intbus1.get());
		System.out.println("Internal Bus 2: "+intbus2.get());
		System.out.println("External Bus 1: "+extbus1.get());
		for (Register r:registersList) {
			System.out.println(r.getRegisterName()+": "+r.getData());
		}
		Scanner entrada = new Scanner(System.in);
		System.out.println("Press <Enter>");
		String mensagem = entrada.nextLine();
	}

	/**
	 * This method uses PC to find, in the memory,
	 * the command code that must be executed.
	 * This command must be stored in IR
	 * NOT TESTED!
	 */
	private void fetch() {
		PC.read();
		memory.read();
		IR.store();
		simulationFetch();
	}

	/**
	 * This method is used to show the components status in simulation conditions
	 * NOT TESTED!!!!!!!!!
	 */
	private void simulationFetch() {
		if (simulation) {
			System.out.println("-------Fetch Phase------");
			System.out.println("PC: "+PC.getData());
			System.out.println("IR: "+IR.getData());
		}
	}

	/**
	 * This method is used to show in a correct way the operands (if there is any) of instruction,
	 * when in simulation mode
	 * NOT TESTED!!!!!
	 * @param instruction 
	 * @return
	 */
	private boolean hasOperands(String instruction) {
		if ("inc".equals(instruction)) //inc is the only one instruction having no operands
			return false;
		else
			return true;
	}

	/**
	 * This method returns the amount of positions allowed in the memory
	 * of this architecture
	 * NOT TESTED!!!!!!!
	 * @return
	 */
	public int getMemorySize() {
		return memorySize;
	}
	
	public static void main(String[] args) throws IOException {
		Architecture arch = new Architecture(true);
		arch.readExec("program");
		arch.controlUnitEexec();
	}
	

}