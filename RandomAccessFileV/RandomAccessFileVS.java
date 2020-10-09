package RandomAccessFileV;
import java.io.*;

//Sector read version.

public class RandomAccessFileVS extends RandomAccessFileV
{
  private static byte[] buf = new byte[512];
  private long TempPos = 0, TempPosV = 0;
  private long base = 0;
  private int r = 0;
  private boolean e = false;

  public RandomAccessFileVS( File file, String mode ) throws FileNotFoundException { super( file, mode ); }
  
  public RandomAccessFileVS( String name, String mode ) throws FileNotFoundException { super( name, mode ); }
  
  //Temporary data. This is so that components that are dependent on this file system can be used without a target file.
  
  public RandomAccessFileVS( byte[] data ) throws IOException { super( data ); }
  
  public RandomAccessFileVS( byte[] data, long Address ) throws IOException { super( data, Address ); }

  //Read and write events.
  
  @Override public int read() throws IOException
  {
    while( Events && Trigger && !Read ) { EventThread.interrupt(); }

    e = super.Events; super.Events = false;

    TempPos = super.getFilePointer(); TempPosV = super.getVirtualPointer();
    
    base = ( TempPos / 512 ) * 512;

    super.seek( base ); super.read( buf ); super.seek( TempPos + 1 );
      
    r = (int)buf[(int)( TempPos - base )];
    
    if( e )
    {
      super.Events = true; Read = true;
      
      super.fireIOEvent( new IOEvent( this, TempPos, TempPos + 1, TempPosV, TempPosV + 1, false ) );
    }

    return( r );
  }
  
  @Override public int read( byte[] b ) throws IOException
  {
    while( Events && Trigger && !Read ) { EventThread.interrupt(); }

    e = super.Events; super.Events = false;

    TempPos = super.getFilePointer(); TempPosV = super.getVirtualPointer();
    
    base = ( TempPos / 512 ) * 512; buf = new byte[(int)( ( ( ( TempPos - base ) + b.length ) / 512 ) + 1 ) * 512];

    super.seek( base ); r = super.read( buf ); super.seek( TempPos + b.length );

    for(int s = (int)( TempPos - base ), e = (int)( s + b.length - 1 ), i = 0; s < e; b[i++] = buf[s++] );

    buf = new byte[512];

    if( e )
    {
      super.Events = true; Read = true;
      
      super.fireIOEvent( new IOEvent( this, TempPos, super.getFilePointer(), TempPosV, super.getVirtualPointer(), false ) );
    }

    return( r );
  }
  
  @Override public int read( byte[] b, int off, int len ) throws IOException
  {
    while( Events && Trigger && !Read ) { EventThread.interrupt(); }

    e = super.Events; super.Events = false;

    TempPos = super.getFilePointer(); TempPosV = super.getVirtualPointer();
    
    base = ( TempPos / 512 ) * 512;

    buf = new byte[(int)( ( ( ( TempPos - base ) + b.length ) / 512 ) + 1 ) * 512];

    super.seek( base ); r = super.read( buf ); super.seek( TempPos + b.length );

    for(int s = (int)( TempPos - base ) + off, e = (int)( s ) + ( len - 1 ), i = 0; s < e; b[i++] = buf[s++] );

    buf = new byte[512];

    if( e )
    {
      super.Events = true; Read = true;
      
      super.fireIOEvent( new IOEvent( this, TempPos, super.getFilePointer(), TempPosV, super.getVirtualPointer(), false ) );
    }
    
    return( r );
  }
}