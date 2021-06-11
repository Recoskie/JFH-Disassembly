package Format;

import swingIO.*;
import swingIO.tree.*;
import javax.swing.tree.*;
import Format.ELFDecode.*;

import core.x86.*;

public class ELF extends Data implements JDEventListener
{
  //Descriptors.

  private Descriptor[][] des = new Descriptor[2][];

  private JDNode root;

  //ELF reader plugins.

  private static Headers header = new Headers();

  JDNode SECHeader;

  //Read the ELF binary.

  public ELF()
  {
    tree.setEventListener( this );

    file.Events = false;

    //Root node is now the target file.
 
    ((DefaultTreeModel)tree.getModel()).setRoot(null); tree.setRootVisible(true); tree.setShowsRootHandles(true); root = new JDNode( fc.getFileName() + ( fc.getFileName().indexOf(".") > 0 ? "" : ".elf" ) );

    //The ELF header.
  
    JDNode ELFHeader = new JDNode( "ELF Header.h", new long[]{ 0, 0 } );
    JDNode PHeader = new JDNode( "Program Header.h", new long[]{ 0, 1 } );
    SECHeader = new JDNode( "Section Header", new long[]{ 1, 0 } );
    
    des[0] = new Descriptor[3];

    try
    {
      des[0][0] = header.readELF(); root.add(ELFHeader); 
      if( !Data.error && Data.programHeader != 0 ) { des[0][1] = header.readProgram(); root.add(PHeader); }
      if( !Data.error && Data.Sections != 0 ) { des[1] = header.readSections(SECHeader); root.add(SECHeader); }
    }
    catch(Exception e) { Data.error = true; }

    if( !Data.error )
    {
      //Load processor core type.

      if( coreType == 0x0003 || coreType == 0x003E )
      {
        if( core == null || core.type() != 0 ){ core = new X86( file ); } else { core.setTarget( file ); }

        core.setBit( is64Bit ? X86.x86_64 : X86.x86_32 );
              
        core.setEvent( this::Dis ); coreLoaded = true;
      }
      else { coreLoaded = false; }

      //Machine code start pos.

      root.add( new JDNode( "Program Start (Machine code).h", new long[]{ -1, start } ) );

      //Decode the setup headers.
    
      ((DefaultTreeModel)tree.getModel()).setRoot(root); file.Events = true;

      //Set the default node.

      tree.setSelectionPath( new TreePath( ELFHeader.getPath() ) ); open( new JDEvent( this, "", new long[]{ 0, 0 } ) );
    }
    else{ file.Events = true; }
  }

  public void open( JDEvent e )
  {
    if( e.getArg(0) < 0 )
    {
      if( e.getArg(0) == -1 )
      {
        if( coreLoaded )
        {
          core.locations.clear(); core.data_off.clear(); core.code.clear();

          core.locations.add( e.getArg(1) );

          core.disLoc(0); ds.setDescriptor( core ); return;
        }
        else { try{ file.seekV( e.getArg(1) ); Virtual.setSelected( e.getArg(1), e.getArg(1) ); } catch(Exception er) { } noCore(); }
      }
      else if( e.getArg(0) == -2 )
      {
        try
        {
          file.seekV( e.getArg(2) );
          file.seek( e.getArg(1) ); //Incase virtual address was overwritten.
          Offset.setSelected( e.getArg(1), e.getArg(1) + e.getArg(3) - 1 );
          Virtual.setSelected( e.getArg(2), e.getArg(2) + e.getArg(3) - 1 );
        } catch( Exception er ) { } 
      }
    }
    else if( e.getArgs().length > 1 ) { ds.setDescriptor( des[ (int)e.getArg(0) ][ (int)e.getArg(1) ] ); }
  }

  public void noCore() { info("<html>The processor core engine is not supported.</html>"); }
}