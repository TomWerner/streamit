package at.dms.kjc.spacedynamic;

import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.FlatVisitor;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.util.Utils;
import java.io.*;
import java.util.List;
import java.util.*;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;


public class FileState implements StreamGraphVisitor, FlatVisitor {
    //true if the graph contains a fileReader
    public boolean foundReader;
    //a hashmap SIRFileReader -> FileReaderDevice
    private HashMap fileReaders;
    //true if the graph contains a fileWriter
    public boolean foundWriter;
    //a hashmap SIRFileWriter -> FileWriterDevice
    private HashMap fileWriters;

    //a hashset containing the flatnodes of all the file manipulators
    //(both readers and writers
    public HashSet fileNodes;
    
    private RawChip rawChip;
    private StreamGraph streamGraph;

    public void visitStaticStreamGraph(StaticStreamGraph ssg) 
    {
	ssg.getTopLevel().accept(this, new HashSet(), false);
    }
	
    public FileState(StreamGraph streamGraph) 
    {
	this.streamGraph = streamGraph;
	this.rawChip = streamGraph.getRawChip();
	foundReader = false;
	foundWriter = false;
	fileReaders = new HashMap();
	fileWriters = new HashMap();
	fileNodes = new HashSet();
	
	streamGraph.getTopLevel().accept(this, null, true);

	//add everything to the fileNodes hashset
	SpaceDynamicBackend.addAll(fileNodes, fileReaders.keySet());
	SpaceDynamicBackend.addAll(fileNodes, fileWriters.keySet());
    }
	
    public void visitNode (FlatNode node) 
    {
	if (node.contents instanceof SIRFileReader) {
	    FileReaderDevice dev =
		new FileReaderDevice(node);
	    IOPort port = getPortFromUser(dev);
	    rawChip.connectDevice(dev, port);
	    
	    fileReaders.put(node, dev);
	    foundReader = true;
	}
	else if (node.contents instanceof SIRFileWriter) {
	    FileWriterDevice dev =
		new FileWriterDevice(node);
	    IOPort port = getPortFromUser(dev);
	    rawChip.connectDevice(dev, port);
	    
	    foundWriter = true;
	    fileWriters.put(node, dev);
	}
    }

    private IOPort getPortFromUser(IODevice dev) 
    {
	BufferedReader inputBuffer = new BufferedReader(new InputStreamReader(System.in));
	int num;
	
	while (true) {
	    System.out.print("Enter port number for " + dev.toString() + ": ");
	    try {
		num = Integer.valueOf(inputBuffer.readLine()).intValue();
	    }
	    catch (Exception e) {
		System.out.println("Bad number!");
		continue;
	    }

	    if (num < 0 || num >= streamGraph.getRawChip().getNumPorts()) {
		System.out.println("Enter valid port number  [0, " + 
				   streamGraph.getRawChip().getNumPorts() + ")\n");
		continue;
	    }
	    
	    if (streamGraph.getRawChip().getIOPort(num).hasDevice()) {
		System.out.println("Port " + num + " already assigned a device!\n");
		continue;
	    }
	    break;
	}
	
	return rawChip.getIOPort(num);
    }

    public Collection getFileWriterDevs() 
    {
	return fileWriters.values();
    }

    public Collection getFileReaderDevs() 
    {
	return fileReaders.values();
    }
    


    public boolean isConnectedToFileReader(RawTile tile) 
    {
	for (int i = 0; i < tile.getIOPorts().length; i++)
	    if (tile.getIOPorts()[i].hasDevice() &&
		tile.getIOPorts()[i].getDevice().isFileReader())
		return true;
	return false;
    }
    
}