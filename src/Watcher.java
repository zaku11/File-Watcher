import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.ArrayList;
import java.util.List;
import static java.nio.file.StandardWatchEventKinds.*;

public class Watcher {
    public List<String> fileList = new ArrayList<>();
    public List<String> directoryList = new ArrayList<>();
    private void compressDirectory(String dir, String zipFile) 
    {
        File directory = new File(dir);
        getFileList(directory);
        try(FileOutputStream inpStream  = new FileOutputStream(new File(zipFile))) 
        {
            ZipOutputStream zipStream = new ZipOutputStream(inpStream);
            for (String filePath : fileList) 
            {
                String name = filePath.substring(directory.getAbsolutePath().length() + 1,filePath.length());
                ZipEntry zipEntry = new ZipEntry(name);
                zipStream.putNextEntry(zipEntry);
                FileInputStream fis = new FileInputStream(filePath);
                byte[] buffer = new byte[1024];
                int length;
                while ((length = fis.read(buffer)) > 0) 
                {
                    zipStream.write(buffer, 0, length);
                }
                zipStream.closeEntry();
                fis.close();
            }
            zipStream.close();
        } 
        catch(FileNotFoundException e)
        {
        	System.out.println("Files couldn't be found. Check again if the paths you gave are correct. Exiting.");
        	System.exit(1);
        }
        catch (IOException e) 
        {
			System.out.println("Error occured-there is probably something wrong with watched file(maybe nonreachable) or zipping failed. Exiting.");
			System.exit(1);
        }
    }
    private void getFileList(File directory) 
    {
        File[] files = directory.listFiles();
        if (files != null && files.length > 0) 
        {
            for (File file : files) 
            {
                if (file.isFile()) 
                {
                    fileList.add(file.getAbsolutePath());
                } 
                else 
                {
                	directoryList.add(file.getAbsolutePath());
                    getFileList(file);
                }
            }
        }
    }
	public static void main(String[] args)
	{
		if(args.length != 2)
		{
			System.out.println("Wrong number of arguments. Exiting " + args.length);
			System.exit(1);
		}
		String pathToWatchedDirectory = args[0];
		String pathToArchiveDirectory = args[1];
		System.out.println("Welcome to my backup application! Just rememeber than after having more than 10 backups, all of them will be erased to preserve space.");
		System.out.println("Watching directory at the path of "+pathToWatchedDirectory);
		System.out.println("Archives will be stored at "+pathToArchiveDirectory);
		while(true)
		{
			try 
			{
				Watcher help = new Watcher();
				help.getFileList(new File(pathToWatchedDirectory));
				WatchService watcher;
				watcher = FileSystems.getDefault().newWatchService();			
				@SuppressWarnings("unused")
				WatchKey key; 
				help.directoryList.add(pathToWatchedDirectory);
				for(String filePath : help.directoryList)
				{
					Path dir = Paths.get(filePath);
					key = dir.register(watcher,ENTRY_CREATE,ENTRY_DELETE,ENTRY_MODIFY);
				}
				try 	
				{
					key = watcher.take();
				}
				catch (InterruptedException x) 
				{
			        return;
			    }
				File[] allFiles = (new File(pathToArchiveDirectory)).listFiles();
				List<File> files = new ArrayList<File>();  
				for(File currfile : allFiles)
		        {
					if(currfile.length() >= 11 && currfile.getName().toString().substring(0,10).equals("Archive : "))
		        	{
		        		files.add(currfile);
		        	}
		        }
				if (files != null && files.size() > 10) 
		        {
					System.out.println("Too many backups. Erasing.");
					for(File currfile : files)
		        	{
						currfile.delete();
		        	}
		        }
				Date date = new Date();
				String pathToArchive = pathToArchiveDirectory + "Archive : "+ (date.toString());
				Watcher zip = new Watcher();
				zip.compressDirectory(pathToWatchedDirectory,pathToArchive.substring(0,pathToArchive.length() - 10));   
				System.out.println("Archive created at "+date.toString().substring(0,date.toString().length()-10)); 
			}
			catch (IOException x) 
			{
				System.out.println("Couldn't create register of events. Exiting");
				System.exit(1);
			}
		}
	}
}
		
