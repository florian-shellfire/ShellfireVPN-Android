package de.shellfire.vpn.android.webservice.test;

import junit.framework.TestCase;
import de.shellfire.vpn.android.webservice.ShellfireWebService;
import de.shellfire.vpn.android.webservice.WsLoginResult;

public class WebServiceTest extends TestCase {
	
	
	public void testLogin() throws Exception {
		ShellfireWebService service = ShellfireWebService.getInstance();
		
		WsLoginResult result = service.login("", "");
		
		System.out.println("loggedIn: " + result.isLoggedIn());
		System.out.println("errorMessage: " + result.getErrorMessage());
		
		assertEquals(result.isLoggedIn(), true);
		assertNull(result.getErrorMessage());
	}
}
