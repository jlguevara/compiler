import org.antlr.runtime.*;
import org.antlr.runtime.tree.*;
import org.antlr.stringtemplate.*;

import java.io.*;
import java.util.Vector;
import java.util.HashMap;
import javax.json.JsonValue;
import javax.json.Json;
import java.util.*;

public class Mini
{
   public static void main(String[] args)
   {
      parseParameters(args);

      CommonTokenStream tokens = new CommonTokenStream(createLexer());
      MiniParser parser = new MiniParser(tokens);
      CommonTree tree = parse(parser);

      if (_displayAST && tree != null)
      {
         DOTTreeGenerator gen = new DOTTreeGenerator();
         StringTemplate st = gen.toDOT(tree);
         System.out.println(st);
      }
      else if (!parser.hasErrors())
      {
         //JsonValue json = translate(tree, tokens);
         //System.out.println(json);

         typeCheck(tree, tokens);
         ControlFlowGraph graph = generateControlFlowGraph(tree, tokens);
         List<BasicBlock> listOfBlocks = graph.getFunBlocks();
         String iloc = getIloc(listOfBlocks);
         System.out.println(iloc);
         if (_dumpIL) {
             writeFile(iloc, _inputFile.replace(".mini", ".il"));
         }
         /*
         X86 x86 = new X86(_inputFile, listOfBlocks, graph.getGlobals());
         String asm = x86.go();
         System.out.println(asm);
         writeFile(asm, _inputFile.replace(".mini", ".s"));
         */
      }
   }

   private static final String DISPLAYAST = "-displayAST";
   private static final String DUMPIL = "-dumpIL";

   private static String _inputFile = null;
   private static boolean _displayAST = false;
   private static boolean _dumpIL = false;

   private static void writeFile(String contents, String filename) {
       Writer writer = null;

       try {
           writer = new BufferedWriter(new FileWriter(filename));
           writer.write(contents);
        }
       catch (IOException ex) {
           ex.printStackTrace();
       }
       finally {
           try {
                writer.close();
            }
           catch (IOException ex) {
               ex.printStackTrace();
           }
       }
   }

   private static String getIloc(List<BasicBlock> blocks) {
        StringBuilder builder = new StringBuilder();
       for (BasicBlock block : blocks) 
       {
           builder.append(getFunctionString(block));
       }
       builder.append("\n");
       return builder.toString();
   }

   /* Generate string for each function */
   private static String getFunctionString(BasicBlock block) {
       StringBuilder builder = new StringBuilder();
       Stack<BasicBlock> stack = new Stack<BasicBlock>();
       HashMap<String, BasicBlock> map = new HashMap<String, BasicBlock>();
       BasicBlock nextBlock;

       stack.push(block);
       map.put(block.getLabel(), block);

       while (!stack.empty()) {
           nextBlock = stack.pop();
           map.put(nextBlock.getLabel(), nextBlock);
           builder.append(getBlockString(nextBlock));
           addChildren(nextBlock, stack, map);
       }
       return builder.toString();
   }

   /* generate iloc string for a basic block */
   private static String getBlockString(BasicBlock block) {
       StringBuilder builder = new StringBuilder();
       List<Instruction> instructions = block.getInstructions();

       builder.append(block + ":\n");
       for (Instruction op : instructions)
            builder.append("\t" + op + "\n");
       
       return builder.toString();
   }

   /* add decendants of basic block */
   private static void addChildren(BasicBlock block, Stack<BasicBlock> stack,
           HashMap<String, BasicBlock> map) {
       List<BasicBlock> lst = block.getOutgoing();
       BasicBlock child;

       // have to traverse the list backwards to get the order right
       for (int i = lst.size() - 1; i >= 0; i--) { 
           child = lst.get(i);

           // skip if child has already been visited
           if (map.get(child.getLabel()) != null)
               continue;

            stack.push(child);
       }
   }

   private static void parseParameters(String [] args)
   {
      for (int i = 0; i < args.length; i++)
      {
         if (args[i].equals(DISPLAYAST))
         {
            _displayAST = true;
         }
         else if (args[i].equals(DUMPIL)) {
             _dumpIL = true;
         }
         else if (args[i].charAt(0) == '-')
         {
            System.err.println("unexpected option: " + args[i]);
            System.exit(1);
         }
         else if (_inputFile != null)
        {
            System.err.println("too many files specified");
            System.exit(1);
         }
         else
         {
            _inputFile = args[i];
         }
      }
   }

   private static CommonTree parse(MiniParser parser)
   {
      try
      {
         MiniParser.program_return ret = parser.program();

         return (CommonTree)ret.getTree();
      }
      catch (org.antlr.runtime.RecognitionException e)
      {
         error(e.toString());
      }
      catch (Exception e)
      {
         System.exit(-1);
      }

      return null;
   }

   private static JsonValue translate(CommonTree tree, CommonTokenStream tokens)
   {
      try
      {
         CommonTreeNodeStream nodes = new CommonTreeNodeStream(tree);
         nodes.setTokenStream(tokens);
         ToJSON tparser = new ToJSON(nodes);

         return tparser.translate();
      }
      catch (org.antlr.runtime.RecognitionException e)
      {
         error(e.toString());
      }
      return Json.createObjectBuilder().build();
   }

   private static void typeCheck(CommonTree tree, CommonTokenStream tokens)
   {
      try
      {
         CommonTreeNodeStream nodes = new CommonTreeNodeStream(tree);
         nodes.setTokenStream(tokens);
         TypeCheck tchecker = new TypeCheck(nodes);

         tchecker.translate();
      }
      catch (org.antlr.runtime.RecognitionException e)
      {
         error(e.toString());
      }
   }

   private static ControlFlowGraph  generateControlFlowGraph(CommonTree tree, 
           CommonTokenStream tokens)
   {
       ControlFlowGraph graph = null;

       try
       {
           CommonTreeNodeStream nodes = new CommonTreeNodeStream(tree);
           nodes.setTokenStream(tokens);
           graph = new ControlFlowGraph(nodes);
           graph.translate();
       }
       catch (org.antlr.runtime.RecognitionException e)
       {
           error(e.toString());
       }
       return graph;
   }

   private static void error(String msg)
   {
      System.err.println(msg);
      System.exit(1);
   }

   private static MiniLexer createLexer()
   {
      try
      {
         ANTLRInputStream input;
         if (_inputFile == null)
         {
            input = new ANTLRInputStream(System.in);
         }
         else
         {
            input = new ANTLRInputStream(
               new BufferedInputStream(new FileInputStream(_inputFile)));
         }
         return new MiniLexer(input);
      }
      catch (java.io.IOException e)
      {
         System.err.println("file not found: " + _inputFile);
         System.exit(1);
         return null;
      }
   }
}
