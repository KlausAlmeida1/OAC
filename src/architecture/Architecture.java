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
	private Memory memory;
	private Memory statusMemory;
	private Register PC;
	private Register IR;
	private Register RPG;
	private Register RPG1;
    private Register RPG2;
    private Register RPG3;
	private Register Flags;
    private Register StackTop;
    private Register StackBotton;
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

		PC = new Register("PC", extbus1, intbus2);
		IR = new Register("IR", extbus1, extbus1);
		RPG = new Register("RPG0", extbus1, intbus1);
		RPG1 = new Register("RPG1", extbus1, intbus1);
		RPG2 = new Register("RPG2", extbus1, intbus1);
		RPG3 = new Register("RPG3", extbus1, intbus1);
		Flags = new Register(2, intbus2);
		StackBotton = new Register("StackBotton", intbus2, intbus2);   
		StackTop = new Register("StackTop", intbus2, intbus2);
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

	public Bus getIntbus2() {
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

    protected Register getRPG1() {
		return RPG1;
	}

    protected Register getRPG2() {
		return RPG2;
	}

    protected Register getRPG3() {
		return RPG3;
	}

	protected Register getFlags() {
		return Flags;
	}

	protected Register getStackTop() {
		return StackTop;
	}

    protected Register getStackBotton() {
		return StackBotton;
	}

	protected Ula getUla() {
		return ula;
	}

	public ArrayList<String> getCommandsList() {
		return commandsList;
	}

	protected boolean getDataStackTop() {
		boolean emptyStack = StackTop.getData() == StackBotton.getData();

		if (emptyStack) 
			return false;

		// StackTop points to a position below  
		int position = StackTop.getData()+1;
		intbus2.put(position);
		StackTop.store();

		// Saved data
		int data = memory.getDataList()[position];

		// Data removed from memory
		memory.getDataList()[position]=0;

		// Bus get the data
		intbus2.put(data);
		return true;
	}

	protected boolean setDataStackTop() {
		boolean fullMemory = memory.getDataList()[StackTop.getData()] != 0;

		if (fullMemory) 
			return false;
		
		// Mem[StackTop] = dataBus
 		int position = StackTop.getData();
		int data = intbus2.get();
		memory.getDataList()[position] = data;

		// StackTop points to a position above 
		intbus2.put(position-1);
		StackTop.store();

		// Replacing the data on the bus
		intbus2.put(data);
		return true;
	}


	//all the microprograms must be impemented here
	
	/**
	 * This method fills the commands list arraylist with all commands used in this architecture
	 */

	protected void fillCommandsList() {    
		commandsList = new ArrayList<String>();
        
		commandsList.add("addRegReg");   // 0 
		commandsList.add("addMemReg");   // 1 
		commandsList.add("addRegMem");   // 2 
		commandsList.add("addImmReg");   // 3  -

		commandsList.add("subRegReg");   // 4 
		commandsList.add("subMemReg");   // 5 
		commandsList.add("subRegMem");   // 6 
		commandsList.add("subImmReg");   // 7 

        commandsList.add("moveMemReg");  // 8 
		commandsList.add("moveRegMem");  // 9 
		commandsList.add("moveRegReg");  // 10 
		commandsList.add("moveImmReg");  // 11         

		commandsList.add("inc");         // 12 

		commandsList.add("jmp");         // 13 
		commandsList.add("jz");          // 14 
		commandsList.add("jn");          // 15 

        commandsList.add("jeq");         // 16   
        commandsList.add("jneq");        // 17 
        commandsList.add("jgt");         // 18 
        commandsList.add("jlw");         // 19 

        commandsList.add("call");        // 20 
        commandsList.add("ret");         // 21 
	}

	protected void pcMaisMais() {
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore();
	}

	// add's

	protected void addRegReg() {
		pcMaisMais();

		// Ula(0) <- REGA
		PC.read(); 
		memory.read();                  // the second register
		demux.setValue(extbus1.get());  //points to the correct register
		registersInternalRead();        //starts the read from the register 
		ula.store(0);

		pcMaisMais();

		// Ula(1) <- REGB
		PC.read();
		memory.read();                  // the second register
		demux.setValue(extbus1.get());  //points to the correct register
		registersInternalRead();        //starts the read from the register
		ula.store(1);

		ula.add();

		// REGB <- UlaAdd
		ula.internalRead(1);
		setStatusFlags(intbus2.get());
		ula.read(1);
		demux.setValue(extbus1.get());   //points to the correct register
		registersInternalStore();

		pcMaisMais();
	}

	protected void addMemReg() {
		pcMaisMais();

		// StackTop <- PC
		PC.internalRead();
		setDataStackTop();
		
		// Ula(0) <- Mem
		PC.read(); 
		memory.read();     // the second register
		memory.read();
		PC.store();
		PC.internalRead();
		ula.internalStore(0);

		// PC <- StackTop
		getDataStackTop();
		PC.internalRead();

		pcMaisMais();
		
		// Ula(1) <- REGA
		PC.read();
		memory.read();                  // the second register
		demux.setValue(extbus1.get());  //points to the correct register
		registersInternalRead();        //starts the read from the register
		ula.store(1);

		ula.add();

		// REGA <- UlaAdd
		ula.internalRead(1);
		setStatusFlags(intbus2.get());
		ula.read(1);
		demux.setValue(extbus1.get()); //points to the correct register
		registersInternalStore();

		pcMaisMais();
	}

	protected void addRegMem() {
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
		setDataStackTop();

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
		getDataStackTop();
		PC.internalStore();
		PC.read();
		memory.read();
		memory.store();
		IR.read();
		memory.store();

		pcMaisMais();
	}

	protected void addImmReg() {
		pcMaisMais();

		//Guardar o valor do PC
		PC.internalRead();
		setDataStackTop();

		//Pegar o valor da memória, passar pelo PC, e meter na ULA
		PC.read();
		memory.read();
		PC.store();
		PC.internalRead();
		ula.internalStore(0);

		//Devolver o valor do PC e fazer o PC ++
		getDataStackTop();
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

	// sub's

	protected void subRegReg() {
		pcMaisMais();

		// Ula(0) <- REGA
		PC.read(); 
		memory.read();                   // the second register
		demux.setValue(extbus1.get());   //points to the correct register
		registersInternalRead();         //starts the read from the register 
		ula.store(0);

		pcMaisMais();

		// Ula(1) <- REGB
		PC.read();
		memory.read();                  // the second register
		demux.setValue(extbus1.get());  //points to the correct register
		registersInternalRead();        //starts the read from the register
		ula.store(1);

		ula.sub();

		// REGB <- UlaSub
		ula.internalRead(1);
		setStatusFlags(intbus2.get());
		ula.read(1);
		demux.setValue(extbus1.get());  //points to the correct register
		registersInternalStore();

		pcMaisMais();
	}

	protected void subMemReg() {
		pcMaisMais();

		// StackTop <- PC
		PC.internalRead();
		setDataStackTop();
		
		// Ula(0) <- Mem
		PC.read(); 
		memory.read();      // the second register
		memory.read();
		PC.store();
		PC.internalRead();
		ula.internalStore(0);

		// PC <- StackTop
		getDataStackTop();
		PC.internalRead();

		pcMaisMais();
		
		// Ula(1) <- REGA
		PC.read();
		memory.read();                  // the second register
		demux.setValue(extbus1.get());  //points to the correct register
		registersInternalRead();        //starts the read from the register
		ula.store(1);
		
		ula.sub();

		// REGB <- UlaSub
		ula.internalRead(1);
		setStatusFlags(intbus2.get());
		ula.read(1);
		demux.setValue(extbus1.get());  //points to the correct register
		registersInternalStore();

		pcMaisMais();
	}

	protected void subRegMem() {
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
		setDataStackTop();

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
		getDataStackTop();
		PC.internalStore();
		PC.read();
		memory.read();
		memory.store();
		IR.read();
		memory.store();

		pcMaisMais();
	}

	protected void subImmReg() {
		pcMaisMais();

		//Guardar o valor do PC
		PC.internalRead();
		setDataStackTop();

		//Pegar o valor da memória, passar pelo PC, e meter na ULA
		PC.read();
		memory.read();
		PC.store();
		PC.internalRead();
		ula.internalStore(0);

		//Devolver o valor do PC e fazer o PC ++
		getDataStackTop();
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

	// move's

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
		setDataStackTop();

		//pegar o valor da memoria e jogar na ula, atravessando o rio nilo e o PC
		PC.read(); 
		memory.read(); 
		memory.read();
		PC.store();
		PC.internalRead();
		ula.internalStore(0);

		//resgatar o valor do PC
		getDataStackTop();
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

		//pegar o endereço de memoria, e dar o comando pro registrador
		//cuspir seu dado, guardando na memoria
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

		// Ula(1) <- REGA
		PC.read();
		memory.read();
		demux.setValue(extbus1.get());   //points to the correct register
		registersInternalRead();
		ula.store(1);

		ula.inc();
		
		// REGA <- UlaInc
		ula.read(1);
		setStatusFlags(intbus1.get());
		demux.setValue(extbus1.get());   //points to the correct register
		registersInternalStore();

		pcMaisMais();
	}

	// Desvios

	public void jmp() {
		pcMaisMais();

		// PC <- Mem
		memory.read();
		PC.store();
	}

	public void stm(int qnt_bit, int a, int b) {
		// stm(1) <- desvio
		PC.read();
		memory.read();
		statusMemory.storeIn1();

		// stm(0) <- PC+1
		ula.inc();
		ula.internalRead(1);
		PC.internalStore();
		PC.read();
		statusMemory.storeIn0();

		int position;

		// Analisando 1 bit
		if (qnt_bit==1) {   // a = bit, b = desvio
			if (Flags.getBit(a) == b) 
				position = statusMemory.getDataList()[1];        // Desvio
			else 
				position = statusMemory.getDataList()[0];        // PC++
		}
		// Analisando 2 bits
		else {   // a = desvio de bit z, b = desvio de bit n
			if (Flags.getBit(0) == a &&
				Flags.getBit(1) == b) 
				position = statusMemory.getDataList()[1];        // Desvio
			else 
				position = statusMemory.getDataList()[0];        // PC++
		}

		extbus1.put(position);
	}

	public void jz() {   // Desvia se bit z=1
		pcMaisMais();

		// PC <- stm
		stm(1,0,1);                  // 1 = um bit, 0 = bit z, 1 = valor de devio
		PC.store();
	}

	public void jn() {   // Desvia se bit n=1
		pcMaisMais();

		// PC <- stm 
		stm(1,1,1);                  // 1 = um bit, 1 = bit n, 1 = valor de desvio
		PC.store();
	}

	public void jRegReg() {
		// Ula(0) <- REGA
		PC.read();
		memory.read();                  // the second register
		demux.setValue(extbus1.get());  //points to the correct register
		registersInternalRead();        //starts the read from the register
		ula.store(0);

		pcMaisMais();

		// Ula(1) <- REGB
		PC.read();
		memory.read();                  // the second register
		demux.setValue(extbus1.get());  //points to the correct register
		registersInternalRead();        //starts the read from the register
		ula.store(1);

		ula.sub();

		ula.internalRead(1);
		setStatusFlags(intbus2.get());
	}
	
	public void jeq() {   // Desvia se RegA==RegB -> bit z=1
		pcMaisMais();
		jRegReg();
		stm(1,0,1);         // 1 = analisa um bit, 0 = bit z, 1 = desvio do bit z
		PC.store();
	}

	public void jneq() {   // Desvia se RegA!=RegB -> bit z=0
		pcMaisMais();
		jRegReg();
		stm(1,0,0);         // 1 = analisa um bit, 0 = bit z, 0 = desvio do bit z
		PC.store();
	}

	public void jgt() {   // Desvia se RegA>RegB -> os dois bis são 0
		pcMaisMais();
		jRegReg();
		stm(2,0,0);         // 2 = analisa 2 bits, 0 = desvio do bit z, 0 = desvio do bit n
		PC.store();
	}

	public void jlw() {   // Desvia se RegA<RegB -> bit z=0, bit n=1
		pcMaisMais();
		jRegReg();
		stm(2,0,1);         // 2 = Analisa 2 bits, 0 = desvio do bit z, 1 = desvio do bit n
		PC.store();
	}

    public void call() {
		pcMaisMais();

		// StackTop <- PC+1
		ula.inc();
		ula.internalRead(1);
		setDataStackTop();

		// PC <- Mem
		PC.read();
		memory.read();
		PC.store();
	}

	public void ret() {
		// PC <- StackTop
		getDataStackTop();
		PC.internalStore();
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
		int i=0, ok=0;

		do {
			linha = br.readLine();
			extbus1.put(i);
			memory.store();
			ok = Integer.parseInt(linha);
			extbus1.put(ok);
			memory.store();
			i++;
		} while (ok != -1);
		
		// Guarda a posição da Stack e a inicializa
		linha = br.readLine();
		int position_Stack = Integer.parseInt(linha);
		initializeStack(position_Stack);

		br.close();
	}

	protected void initializeStack(int position) {	
		intbus2.put(position);
		StackTop.store();
		StackBotton.store();
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
		int command = extbus1.get();
		simulationDecodeExecuteBefore(command);

		switch (command) {
			case 0: addRegReg(); break;
			case 1: addMemReg(); break;
			case 2: addRegMem(); break;
			case 3: addImmReg(); break;

			case 4: subRegReg(); break;
			case 5: subMemReg(); break;
			case 6: subRegMem(); break;
			case 7: subImmReg(); break;

			case 8:  moveMemReg(); break;
			case 9:  moveRegMem(); break;
			case 10: moveRegReg(); break;
			case 11: moveImmReg(); break;

			case 12: inc(); break;

			case 13: jmp(); break;
			case 14: jz();  break;
			case 15: jn();  break;

			case 16: jeq();  break;
			case 17: jneq(); break;
			case 18: jgt();  break;
			case 19: jlw();  break;

			case 20: call(); break;
			case 21: ret();  break;

			default: halt = true; break;
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
		int parameter  = 0;
		int parameter2 = 0;
		int parameter3 = 0;

		for (Register r:registersList) {
			System.out.println(r.getRegisterName()+": "+r.getData());
		}

		if (command !=-1)
			instruction = commandsList.get(command);
		else
			instruction = "END";
			
		if (hasOperands(instruction)) {
			parameter = memory.getDataList()[PC.getData()+1];

			if (hasOneOperand(command)) 
				System.out.println("Instruction: "+instruction+" "+parameter);
			else {
				parameter2 = memory.getDataList()[PC.getData()+2];

				if (hasTwoOperands(command)) 
					System.out.println("Instruction: "+instruction+" "+parameter+
																   " "+parameter2);
				else {
					parameter3 = memory.getDataList()[PC.getData()+3];
					System.out.println("Instruction: "+instruction+" "+parameter+
																   " "+parameter2+
																   " "+parameter3);
				}
			}
		}
		else 
			System.out.println("Instruction: "+instruction);
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
		if ("ret".equals(instruction))   //ret is the only one instruction having no operands
			return false;
		else
			return true;
	}

	private boolean hasOneOperand(int command) {
		if (command>=12 && command<=15 ||
			command==20)   
			return true;
		else
			return false;
	}

	private boolean hasTwoOperands(int command) {
		if (command>=0 && command<=11)   
			return true;
		else
			return false;
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
		arch.readExec("t2");
		arch.controlUnitEexec();
	}
}
