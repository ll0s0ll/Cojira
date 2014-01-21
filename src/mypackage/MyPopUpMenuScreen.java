package mypackage;

import net.rim.device.api.command.*;
import net.rim.device.api.system.*;
import net.rim.device.api.ui.*;
import net.rim.device.api.ui.component.*;
import net.rim.device.api.ui.container.*;
import net.rim.device.api.ui.menu.*;
import net.rim.device.api.ui.image.*;
import net.rim.device.api.util.*;
import java.util.*;

/*
public class MyPopUpMenuApp extends UiApplication 
{
    public static void main(String[] args)
    {
        MyPopUpMenuApp theApp = new MyPopUpMenuApp();
        theApp.enterEventDispatcher();
    }
    
    public MyPopUpMenuApp()
    {
        pushScreen(new MyPopUpMenuScreen());
    }
}
*/
public class MyPopUpMenuScreen extends MainScreen
{
	
	EmailAddressEditField emailAddress;
    public MyPopUpMenuScreen()
    {       
        setTitle("Pop-Up Menu Demo");
        setContextMenuProvider(new DefaultContextMenuProvider());
        
        LabelField labelField = new LabelField("Click to invoke pop-up menu", Field.FOCUSABLE);
        emailAddress = new EmailAddressEditField("Email address: ", "name@blackberry.com", 40);
                
        ItemProvider itemProvider = new ItemProvider();
                       
        labelField.setCommandItemProvider(itemProvider);
        emailAddress.setCommandItemProvider(itemProvider);
                
        add(labelField);
        add(emailAddress);
    }
    
    protected void makeMenu( Menu menu, int instance )
    {        
        MenuItem _prog = new MenuItem(new StringProvider("選局"), 0x00020000, 0);
        _prog.setCommand(new Command(        
	        new CommandHandler()
	        {
	        	public void execute(ReadOnlyCommandMetadata metadata, Object context)
	            {
	        		MyPopUpMenuScreen _screen = new MyPopUpMenuScreen();	        		
	        		getUiEngine().pushScreen(_screen);	        		
	            }        	
	        }
        ));
        
        menu.add(_prog);
        
        
        super.makeMenu(menu, instance);
    };
    
    /* To override the default functionality that prompts the user to save changes before the application closes, 
     * override the MainScreen.onSavePrompt() method. In the following code sample, the return value is true which 
     * indicates that the application does not prompt the user before closing.
     */ 
    protected boolean onSavePrompt()
    {
        return true;
    }
    class ItemProvider implements CommandItemProvider 
    {
        public Object getContext(Field field)
        {            
            return field;
        }
        public Vector getItems(Field field)
        {            
            Vector items = new Vector();
            
            CommandItem defaultCmd;
            
            Image myIcon = ImageFactory.createImage(Bitmap.getBitmapResource("icon.png"));
            
            if(field.equals(emailAddress)){          
                 defaultCmd = new CommandItem(new StringProvider("Email Address"), null, new Command(new DialogCommandHandler()));
            }
            else{
                defaultCmd = new CommandItem(new StringProvider("選局/停止"), null, new Command(new DialogCommandHandler()));
            }

            items.addElement(defaultCmd);
            items.addElement(new CommandItem(new StringProvider("番組表"), null, new Command(new DialogCommandHandler())));
            //items.addElement(new CommandItem(new StringProvider("現在の番組"), null, new Command(new DialogCommandHandler())));
            //items.addElement(new CommandItem(new StringProvider("番組表"), null, new Command(new DialogCommandHandler())));
            //items.addElement(new CommandItem(new StringProvider("停止"), null, new Command(new DialogCommandHandler())));
            
            return items;
        }
    }
    class DialogCommandHandler extends CommandHandler
    {
        public void execute(ReadOnlyCommandMetadata metadata, Object context)
        {
            Dialog.alert("Executing command for " + context.toString());
        }           
    }
}