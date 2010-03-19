package org.thomnichols.pythonwebconsole;

import static org.thomnichols.pythonwebconsole.Util.validateCaptcha;
import static org.thomnichols.pythonwebconsole.Util.validateParam;

import java.io.IOException;

import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thomnichols.pythonwebconsole.model.Comment;
import org.thomnichols.pythonwebconsole.model.Script;

public class CommentServlet extends HttpServlet {
	final Logger log = LoggerFactory.getLogger( getClass() );

	private PersistenceManagerFactory pmf;
	private String recapPrivateKey;
	private boolean debug;

	@Override
	public void init() throws ServletException {
		this.pmf = (PersistenceManagerFactory)super.getServletContext().getAttribute( "persistence" );
		this.recapPrivateKey = (String)getServletContext().getAttribute( "recaptchaPrivateKey" );
		this.debug = (Boolean)getServletContext().getAttribute( "debug" );
	}
	
	@Override
	protected void doPost( HttpServletRequest req, HttpServletResponse resp )
			throws ServletException, IOException {
		String captcha = req.getParameter( "junk" ); 
		if ( captcha != null && ! captcha.trim().equals("") ) { // simple captcha
			resp.sendError( 400, "Boooo!!" );
			return;
		}
		
		PersistenceManager pm = pmf.getPersistenceManager();
		try {
        	if ( ! debug ) validateCaptcha( recapPrivateKey, req );
			String scriptID = validateParam( req, "script_id" );
			String title = validateParam( req, "title" );
			String commentText = El.esc( validateParam( req, "content" ) );
			String author = req.getParameter( "author" );
			
			pm.getObjectById( Script.class, scriptID ); // verify script exists.
			
			Comment comment = new Comment( scriptID, author, title, commentText );
			pm.makePersistent( comment );
			log.debug( "Saved comment from: " + comment.getAuthor() );
			// TODO how to represent this in the response?
			resp.setStatus( 201 );
			resp.getWriter().append( "Okey dokey." );
		}
        catch ( JDOObjectNotFoundException ex ) {
        	resp.sendError( 400, "Invalid script ID" );
        }
		catch ( ValidationException ex ) {
			log.warn( "Validation failed", ex );
			resp.sendError( 400, "Validation failed " + ex.getMessage() );
		}
		finally { pm.close(); }
	}
}
