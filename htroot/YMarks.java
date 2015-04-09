import net.yacy.cora.protocol.RequestHeader;
import de.anomic.data.UserDB;
import de.anomic.data.ymark.YMarkTables;
import de.anomic.search.Switchboard;
import de.anomic.server.serverObjects;
import de.anomic.server.serverSwitch;

public class YMarks {
	public static serverObjects respond(final RequestHeader header, final serverObjects post, final serverSwitch env) {
        final Switchboard sb = (Switchboard) env;
        final serverObjects prop = new serverObjects();
        final UserDB.Entry user = sb.userDB.getUser(header);
        final boolean isAdmin = (sb.verifyAuthentication(header, true));
        final boolean isAuthUser = user!= null && user.hasRight(UserDB.AccessRight.BOOKMARK_RIGHT);
        
        if(isAdmin || isAuthUser) {
        	prop.put("login", 1);
        	String bmk_user = (isAuthUser ? user.getUserName() : YMarkTables.USER_ADMIN);
        	prop.putHTML("user", bmk_user.substring(0,1).toUpperCase() + bmk_user.substring(1));
        	
        } else {
        	prop.put("login", 0);
        }
        
        return prop;
	}
}