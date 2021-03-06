/*
 * @(#)LocalCopy.java
 *
 * Copyright (c) 2001-2002, JangHo Hwang
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 	1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * 	2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 	3. Neither the name of the JangHo Hwang nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *    $Id: LocalCopy.java,v 1.11 2005/05/01 23:55:54 xrath Exp $
 */
package rath.msnm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;

import rath.msnm.entity.Group;
import rath.msnm.entity.MsnFriend;
import rath.msnm.msg.MimeUtility;
/**
 * 保存本地copy，这样不用每次都去读取。
 * 暂时看不出好处。
 * 如何处理update呢？
 * @author sheng.liuzs@alipay.com
 * @version $Id: LocalCopy.java, v 0.1 2008-10-4 下午02:08:51 sheng.liuzs@alipay.com Exp $
 */
public class LocalCopy
{
    //默认在C:\Documents and Settings\Administrator\.jmsn下面来保存本地的copy
	public static final File DEFAULT_HOME_DIR = new File(System.getProperty("user.home"), ".jmsn");
	private final File homedir;
	private File userdir = null;
	private String loginName = null;

	private Properties setting = new Properties();

	
	public LocalCopy()
	{
		homedir = DEFAULT_HOME_DIR;
		homedir.mkdirs();
	}

	public LocalCopy( File dir )
	{
		if( dir.isFile() )
			throw new IllegalArgumentException("dir is must be directory");
		homedir = dir;
		homedir.mkdirs();
	}

	public File getHomeDirectory()
	{
		return this.homedir;
	}

	
	public void setLoginName( String loginName )
	{
		this.loginName = loginName;

		userdir = new File( homedir, loginName.toLowerCase() );
		userdir.mkdirs();
	}

	public String getLoginName()
	{
		return this.loginName;
	}

	//把setting.prop里面的信息装载进一个Properties里面
	public void loadInformation()
	{
		if( userdir==null )
			throw new IllegalStateException( "required setLoginName" );

		File file = new File( userdir, "setting.prop" );
		if( !file.exists() )
			return;

		FileInputStream fis = null;
		try
		{
			fis = new FileInputStream(file);
			setting.load( fis );
			fis.close();
		}
		catch( IOException e ) {
			System.err.println( "cannot read local setting file: " + e );
		}
		finally
		{
			if( fis!=null )
			{
				try
				{
					fis.close();
				} catch( IOException e ) {}
			}
		}
	}

	public void setProperty( String key, String value )
	{
		setting.setProperty(key, value);
	}

	public void setProperty( String key, boolean value )
	{
		setProperty( key, String.valueOf(value) );
	}

	public String getProperty( String key )
	{
		return setting.getProperty(key);
	}

	public String getProperty( String key, String def )
	{
		return setting.getProperty(key, def);
	}

	/**
	 * If key is undefined, this will return false.
	 */
	public boolean getPropertyBoolean( String key )
	{
		String value = getProperty(key);
		if( value==null )
			return false;
		return Boolean.valueOf(value).booleanValue();
	}

	public boolean getPropertyBoolean( String key, boolean def )
	{
		String value = getProperty(key);
		if( value==null )
			return def;
		return Boolean.valueOf(value).booleanValue();
	}

	public void storeInformation()
	{
		FileOutputStream fos = null;

		try
		{
			fos = new FileOutputStream(new File(userdir, "setting.prop").getAbsolutePath());
			setting.store( fos, "JMSN setting" );
			fos.flush();
			fos.close();
			fos = null;
		}
		catch( IOException e )
		{
			System.err.println( "cannot write serial infomration file: " + e );
		}
		finally
		{
			if( fos!=null )
			{
				try { fos.close(); } catch( Exception e ) {}
			}
		}
	}

	/**
	 *
	 */
	public void storeBuddies( BuddyGroup bg )
	{
		try
		{
			writeGroups( bg.getGroupList(), new File(userdir, "Groups.prop") );
			writeList( bg.getForwardList(), new File(userdir,"Forward"), true );
			writeList( bg.getReverseList(), new File(userdir,"Reverse") );
			writeList( bg.getAllowList(), new File(userdir,"Allow") );
			writeList( bg.getBlockList(), new File(userdir,"Block") );
		}
		catch( Exception e )
		{
			System.err.println( "cannot write buddy list cache: " + e );
		}
	}

	public void loadBuddies( BuddyGroup bg )
	{
		try
		{
			readGroups( bg.getGroupList(), new File(userdir,"Groups.prop") );
			readList( bg.getForwardList(), new File(userdir,"Forward"), true );
			readList( bg.getReverseList(), new File(userdir,"Reverse") );
			readList( bg.getAllowList(), new File(userdir,"Allow") );
			readList( bg.getBlockList(), new File(userdir,"Block") );
		}
		catch( Exception e )
		{
			System.err.println( "cannot read buddy list cache: " + e );
		}
	}

	private void readList( BuddyList list, File file ) throws IOException
	{
		readList( list, file, false );
	}

	private void readList( BuddyList list, File file, boolean isFL ) throws IOException
	{
		list.clear();
		if( !file.exists() )
			return;

		FileInputStream fis = null;
		try
		{
			fis = new FileInputStream(file);
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));
			String line = null;
			while( (line=br.readLine())!=null )
			{
				MsnFriend friend = null;
				if( isFL )
				{
					String[] rec = line.split(",", 4);
					friend = new MsnFriend(rec[0], rec[3]);
					friend.setGroupIndex( rec[2] );
					friend.setCode( rec[1] );
				}
				else
				{
					String[] rec = line.split(",", 2);
					friend = new MsnFriend(rec[0], rec[1]);
				}
				list.add( friend );
			}
			br.close();
			fis.close();
			fis = null;
		}
		finally
		{
			if( fis!=null )
				fis.close();
		}
	}

	private void writeList( BuddyList list, File file ) throws IOException
	{
		writeList( list, file, false );
	}

	private void writeList( BuddyList list, File file, boolean isFL ) throws IOException
	{
		FileOutputStream fos = null;
		try
		{
			fos = new FileOutputStream(file);
			PrintWriter out = new PrintWriter(fos);
			for(Iterator i=list.iterator(); i.hasNext(); )
			{
				MsnFriend friend = (MsnFriend)i.next();
				out.print( friend.getLoginName() );
				if( isFL )
				{
					out.print( "," );
					out.print( friend.getCode() );
					out.print( "," );
					out.print( friend.getGroupIndex() );
				}
				out.print( "," );
				out.print( friend.getFriendlyName() );
				out.println();
			}
			out.flush();
			out.close();
			fos.close();
			fos = null;
		}
		finally
		{
			if( fos!=null )
				fos.close();
		}
	}

	private void writeGroups( GroupList list, File file ) throws IOException
	{
		FileOutputStream fos = null;
		Properties prop = new Properties();
		for(Iterator i=list.iterator(); i.hasNext(); )
		{
			Group group = (Group)i.next();
			prop.setProperty( "group." + group.getIndex(), group.getName() );
		}

		try
		{
			fos = new FileOutputStream(file);
			prop.store( fos, "JMSN Group cache file" );
			fos.flush();
			fos.close();
			fos = null;
		}
		finally
		{
			if( fos!=null )
				fos.close();
		}
	}

	private void readGroups( GroupList list, File file ) throws IOException
	{
		list.clear();
		if( !file.exists() )
			return;

		FileInputStream fis = null;
		Properties prop = new Properties();

		try
		{
			fis = new FileInputStream(file);
			prop.load( fis );

			for(Enumeration e=prop.propertyNames(); e.hasMoreElements(); )
			{
				String key = (String)e.nextElement();
				String value = MimeUtility.getURLDecodedString(prop.getProperty(key), "UTF-8");

				if( key.indexOf('.')==-1 )
					continue;

				String index = key.substring( key.indexOf('.')+1 );
				Group group = new Group( value, index );

				list.addGroup( group );
			}
		}
		finally
		{
			if( fis!=null )
				fis.close();
		}
	}
}
