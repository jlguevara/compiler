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
         List<BasicBlock> listOfBlocks = generateControlFlowGraph(tree, tokens);
         String iloc = getIloc(listOfBlocks);
         System.out.println(iloc);
         if (_dumpIL) {
             writeFile(iloc, _inputFile.replace(".mini", ".il"));
         }
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

   private static String getFunctionString(BasicBlock block) 
   {
       StringBuilder builder = new StringBuilder();
       Queue<BasicBlock> queue = new LinkedList<BasicBlock>();
       HashMap<String, BasicBlock> map = new HashMap<String, BasicBlock>();
       List<BasicBlock> links;
       BasicBlock nextBlock, tmp;

       queue.add(block);
       map.put(block.getLabel(), block);

       while (!queue.isEmpty()) {
           nextBlock = queue.remove();
           builder.append(getBlockString(nextBlock));
           links = nextBlock.getOutgoing();
           for (int i = 0; i < links.size(); i++) {
               tmp = links.get(i);
               if (map.get(tmp.getLabel()) == null) {
                    queue.add(tmp);
                    map.put(tmp.getLabel(), tmp);
               }
           }
       }
       return builder.toString();
   }

   private static String getBlockString(BasicBlock block) {
       StringBuilder builder = new StringBuilder();
       List<Instruction> instructions = block.getInstructions();

       builder.append(block + ":\n");
       for (Instruction op : instructions)
            builder.append("\t" + op + "\n");
       
       return builder.toString();
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

   private static List<BasicBlock> generateControlFlowGraph(CommonTree tree, 
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
        
       return graph.getFunBlocks();
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
