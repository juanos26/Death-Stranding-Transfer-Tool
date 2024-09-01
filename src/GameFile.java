import java.io.File;

public class GameFile {

    // Variables
    private File OGgameFile;//Location of the original game file
    private File newMacFile;//Location of Mac version of the game file
    private File newWinFile;//Location of Windows version of the game file
    private String osName = System.getProperty("os.name");//Get the current OS name to import

    // Constructor
    public GameFile(File oldFile){
        this.OGgameFile = oldFile;
    }

    // Methods

    //Get the Mac version of the game file
    public File getMacGameFile(){
        if (!OGgameFile.getName().toLowerCase().endsWith(".bundle") && OGgameFile.isDirectory()){

            

        } else if (OGgameFile.getName().toLowerCase().endsWith(".bundle")) {
            return OGgameFile;//Returning orginal mac file
        }else{
            System.out.println("This is not a valid game file");
        }
    }

    //Get the Windows version of the game file
    public File getWinGameFile(){
        if (OGgameFile.isDirectory()){

        } else if (OGgameFile.getName().toLowerCase().endsWith(".dat")) {

        }else{
            System.out.println("This is not a valid game file");
        }
    }


}
