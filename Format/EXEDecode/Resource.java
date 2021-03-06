package Format.EXEDecode;
import java.io.*;
import swingIO.*;
import swingIO.tree.*;

public class Resource extends Data implements sec
{
  //Data structure data descriptors.

  java.util.LinkedList<Descriptor> des = new java.util.LinkedList<Descriptor>();

  //Read files, and folders.

  int ref = 0;

  //Each DIR contains a list to another Dir list, or File.

  JDNode nDir;
  Descriptor File_Str;
  long t = 0;

  //Use current IO potion if no defined position.

  public Descriptor[] read( JDNode Dir ) throws IOException { return( read( Dir, 0 ) ); }

  //Recursively read Resource at a set position.

  public Descriptor[] read( JDNode Dir, long pos ) throws IOException
  {
    Descriptor des_Dir;

    //Number of DIR/File locations under this DIR.

    int  size = 0;
    
    //The position of the current DIR.
    
    long Pos = pos;

    //The position of the DIR. IF not used then current IO position is used.

    if( pos != 0 ) { file.seekV( pos ); }

    //The DIR info descriptor.

    des_Dir = new Descriptor( file, true ); des_Dir.setEvent( this::dirInfo );

    des_Dir.LUINT32("Characteristics");
    des_Dir.LUINT32("Time Date Stamp");
    des_Dir.LUINT16("Major Version");
    des_Dir.LUINT16("Minor Version");
    des_Dir.LUINT16("Number Of Named Entries"); size = ((Short)des_Dir.value).intValue();
    des_Dir.LUINT16("Number Of Id Entries"); size += ((Short)des_Dir.value).intValue();

    des.add(des_Dir);

    Dir.add( new JDNode( "Directory Info.h", new long[]{ 4, ref } )); ref += 1;

    for( int i = 0; i < size; i++ )
    {
      des_Dir.Array("Array Element " + i + "", 8 );

      des_Dir.LINT32("Name, or ID");

      t = ( (Integer)des_Dir.value );

      //Negative value locates to a string name.

      if( t < 0 )
      {
        pos = t & 0x7FFFFFFF; t = file.getVirtualPointer(); file.seekV( pos + DataDir[4] );

        File_Str = new Descriptor( file, true ); des.add( File_Str ); File_Str.setEvent( this::strInfo );

        File_Str.LUINT16("Name length"); File_Str.LString16("Entire Name", ((Short)File_Str.value).intValue() );
        
        nDir = new JDNode( File_Str.value.toString(), new long[]{ 4, ref } ); ref += 1;

        file.seekV( t );
      }

      //Positive value is a ID name.

      else { nDir = new JDNode( t + "" ); }

      Dir.add( nDir );

      des_Dir.LINT32("Directory, or File");
      
      pos = ((Integer)des_Dir.value).intValue();

      //Factorial.

      if( pos < 0 )
      {
        Pos = file.getVirtualPointer();
        
        read( nDir, ( pos & 0x7FFFFFFF ) + DataDir[4] );
        
        file.seekV( Pos );
      }
      
      //File info.
      
      else
      {
        nDir.add( new JDNode( "File Info.h", new long[]{ 4, ref } )); ref += 1;
        
        t = file.getVirtualPointer(); file.seekV( pos + DataDir[4] );

        File_Str = new Descriptor( file, true ); File_Str.setEvent( this::fileInfo );

        File_Str.LUINT32("File location"); pos = ((Integer)File_Str.value).longValue() + imageBase;
        File_Str.LUINT32("File size"); nDir.add( new JDNode( "File Data", new long[]{ -4, pos, pos + ( ( (Integer)File_Str.value ).longValue() ) - 1 } ) );
        File_Str.LUINT32("Code Page");
        File_Str.LUINT32("Reserved");

        des.add( File_Str );

        file.seekV( t );
      }
    }

    return( des.toArray( new Descriptor[ des.size() ] ) );
  }

  public static final String res = "A section that is reserved, is skipped. So that some day the empty space may be used for something new.";

  public static final String Ver = "Major, and Minor are put together to forum the version number.<br /><br />Example.<br /><br />Major version = 5<br /><br />Minor version = 12<br /><br />Would mean version 5.12V.";

  public static final String[] DirInfo = new String[] {"<html>Characteristics are reserved, for future use.<br /><br />" + res + "</html>",
    "<html>A date time stamp is in seconds. The seconds are added to the starting date \"Wed Dec 31 7:00:00PM 1969\".<br /><br />" +
    "If the time date stamp is \"37\" in value, then it is plus 37 seconds giving \"Wed Dec 31 7:00:37PM 1969\".</html>",
    "<html>" + Ver + "</html>",
    "<html>" + Ver + "</html>",
    "<html>Number of files, or folders with names.<br /><br />Named entires, and numeral named ID are added together for array size.</html>",
    "<html>Number of files, or folders with numerical names.<br /><br />Named entires, and numeral named ID are added together for array size.</html>",
    "<html>File, or Folder array element.</html>",
    "<html>If the value is positive. The number value is the name.<br /><br />" +
    "If the value is negative. Flip the number from negative to positive. Subtract the value into 2147483648. This removes the sing.<br /><br />" +
    "The location is added to the start of the resource section. The string at that location is then the name, of the folder, or file.</html>",
    "<html>If the value is positive. It is a location to a file.<br /><br />" +
    "If the value is negative. Flip the number from negative to positive. Subtract the value into 2147483648. This removes the sing.<br /><br />" +
    "The location is added to the start of the resource section. The location locates to anther Directory of files, or folders.</html>",
  };

  public void dirInfo( int el )
  {
    if( el < 0 )
    {
      info( "<html>A directory consisting, of characteristics, time date stamp, and number of files, or folders.</html>" );
    }
    else
    {
      if( el > 6 ) { el = ( ( el - 6 ) % 3 ) + 6; }

      info( DirInfo[ el ] );
    }
  }

  public static final String[] FileInfo = new String[] {"<html>The location to the file. This location is added to the base address of the program.</html>",
    "<html>The size of the file.</html>",
    "<html></html>",
    "<html>" + res + "</html>"
  };

  public void fileInfo( int el )
  {
    if( el < 0 )
    {
      info( "<html>Each file location. Has a location to the actual data, size, and code page.</html>" );
    }
    else
    {
      info( FileInfo[ el ] );
    }
  }

  public static final String[] StrInfo = new String[] {"<html>The character length of the string. Each character is 16 bits.</html>",
    "<html>The name of the folder, or file.</html>"
  };

  public void strInfo( int el )
  {
    if( el < 0 )
    {
      info( "<html>Location to the named Folder, or File.</html>" );
    }
    else
    {
      info( StrInfo[ el ] );
    }
  }
}
