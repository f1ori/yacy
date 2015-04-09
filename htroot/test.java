//
//$LastChangedDate$
//$LastChangedRevision$
//$LastChangedBy$
//

import net.yacy.cora.protocol.RequestHeader;
import de.anomic.server.serverObjects;
import de.anomic.server.serverSwitch;

public class test {
    
    // http://localhost:8090/test.xml?count=10
    
    public static serverObjects respond(final RequestHeader header, final serverObjects post, final serverSwitch env) {
        final serverObjects prop = new serverObjects();
        final int count = Math.min(1000, (post == null) ? 0 : post.getInt("count", 0));
        
        for (int i = 0; i < count; i++) {
            prop.put("item_" + i + "_text", Integer.toString(i));
        }
        prop.put("item", count);

        return prop;
    }
    
}
