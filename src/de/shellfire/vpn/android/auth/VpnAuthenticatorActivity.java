package de.shellfire.vpn.android.auth;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import de.shellfire.vpn.android.R;
import de.shellfire.vpn.android.RegisterActivity;
import de.shellfire.vpn.android.openvpn.VpnStatus;
import de.shellfire.vpn.android.webservice.ShellfireWebService;

/**
 * Activity which displays a login screen to the user, offering registration as
 * well.
 */
public class VpnAuthenticatorActivity extends AccountAuthenticatorActivity {

	private static final String REGISTERACTIVITYSHOWN2 = "REGISTERACTIVITYSHOWN";
	public final static String ARG_AUTH_TYPE = "AUTH_TYPE";
	public final static String ARG_ACCOUNT_NAME = "ACCOUNT_NAME";
	public final static String ARG_IS_ADDING_NEW_ACCOUNT = "IS_ADDING_ACCOUNT";

	public final int REGISTER = 124329;

	/**
	 * The default email to populate the email field with.
	 */
	public static final String EXTRA_EMAIL = "de.shellfire.vpn.android.auth.EMAIL";

	public static final String KEY_ERROR_MESSAGE = "ERR_MSG";

	public final static String PARAM_USER_PASS = "USER_PASS";
	public final static String PARAM_USER_HAS_LOGIN = "PARAM_USER_HAS_LOGIN";
	
	private AccountManager mAccountManager;
	private String mAuthTokenType;

	/**
	 * Keep track of the login task to ensure we can cancel it if requested.
	 */
	private UserLoginTask mAuthTask = null;

	// Values for email and password at the time of the login attempt.
	private String mUser;
	private String mPassword;

	// UI references.
	private EditText mEmailView;
	private EditText mPasswordView;
	private View mLoginFormView;
	private View mLoginStatusView;
	private TextView mLoginStatusMessageView;

	private ShellfireWebService webService;
	private String mToken;
	private VpnAuthenticatorActivity mContext;
	private boolean registerActivityShown;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState != null) {
			registerActivityShown = savedInstanceState.getBoolean(REGISTERACTIVITYSHOWN2);
			
		}
		
		
		setContentView(R.layout.activity_login);
		mAccountManager = AccountManager.get(getBaseContext());
		mContext = this;
		String accountName = getIntent().getStringExtra(AccountGeneral.ACCOUNT_NAME);
		mAuthTokenType = getIntent().getStringExtra(ARG_AUTH_TYPE);
		if (mAuthTokenType == null)
			mAuthTokenType = AccountGeneral.AUTHTOKEN_TYPE_USE_VPN;

		if (accountName != null) {
			((TextView) findViewById(R.id.email)).setText(accountName);
		}

		webService = ShellfireWebService.getInstance();

		// Set up the login form.
		mUser = getIntent().getStringExtra(EXTRA_EMAIL);
		mEmailView = (EditText) findViewById(R.id.email);

		mPasswordView = (EditText) findViewById(R.id.password);
		mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
				if (id == R.id.login || id == EditorInfo.IME_NULL) {
					attemptLoginChecked();
					return true;
				}
				return false;
			}
		});

		mLoginFormView = findViewById(R.id.login_form);
		mLoginStatusView = findViewById(R.id.login_status);
		mLoginStatusMessageView = (TextView) findViewById(R.id.register_status_message);

		findViewById(R.id.register_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				attemptLoginChecked();
			}
		});
		
		if (!registerActivityShown) {
			showRegisterActivity();
			registerActivityShown = true;
		}
	}
	
	public void onSaveInstanceState(Bundle outState) {
		outState.putBoolean(REGISTERACTIVITYSHOWN2, registerActivityShown);
		super.onSaveInstanceState(outState);
	}


	public void lostAccountDataClicked(View view) {
		String url = webService.getUrlPasswordLost();

		Uri uri = Uri.parse(url);
		startActivity(new Intent(Intent.ACTION_VIEW, uri));
	}

	public void showRegisterActivity() {

		Intent intent = new Intent(getBaseContext(), RegisterActivity.class);
		startActivityForResult(intent, REGISTER);

	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		return webService;
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// Check which request we're responding to
		if (requestCode == REGISTER) {
			if (resultCode == RESULT_OK) {
				// cool, we now have an account try to login with this data
				Bundle bundle = data.getExtras();
				boolean userHasLogin = bundle.getBoolean(PARAM_USER_HAS_LOGIN);
				if (!userHasLogin) {
					mUser = bundle.getString(AccountManager.KEY_ACCOUNT_NAME);
					mPassword = bundle.getString(PARAM_USER_PASS);
					mEmailView.setText(mUser);
					mPasswordView.setText(mPassword);
					mToken = bundle.getString("token");
					showProgress(true);
					mAuthTask = new UserLoginTask();
					mAuthTask.execute((Void) null);
				}
				

			} else {
				// cancelled or did not work, doing nothing
			}

		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.login, menu);
		return true;
	}

	private boolean isInternetAvailable() {
		ConnectivityManager cm =
		        (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
		 
		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		boolean isConnected = activeNetwork != null &&
		                      activeNetwork.isConnectedOrConnecting();
		
		return isConnected;
	}
	
	
	public void attemptLoginChecked() {
		Runnable action = new Runnable() {
			public void run() {
				attemptLogin();
			}
		};
		if (isInternetAvailable()) {
			action.run();
		} else {
			showDialogInternetRequired(R.string.retry, action);
		}
		
		
	}
	
	private void showDialogInternetRequired(int id, final Runnable action) {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle(getString(R.string.network_problem_title));
		alert.setMessage(getString(R.string.network_problem_message));
		alert.setPositiveButton(getString(id),new OnClickListener() {

			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				if (isInternetAvailable()) {
						action.run();
				} else {
					showDialogInternetRequired(R.string.retry, action);
				}
			}
			
		});
		alert.show();  		
	}
	
	
	/**
	 * Attempts to sign in or register the account specified by the login form.
	 * If there are form errors (invalid email, missing fields, etc.), the
	 * errors are presented and no actual login attempt is made.
	 */
	public void attemptLogin() {
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(mEmailView.getWindowToken(), 0);

		if (mAuthTask != null) {
			return;
		}

		// Reset errors.
		mEmailView.setError(null);
		mPasswordView.setError(null);

		// Store values at the time of the login attempt.
		mUser = mEmailView.getText().toString();
		mPassword = mPasswordView.getText().toString();

		boolean cancel = false;
		View focusView = null;

		// Check for a valid password.
		if (TextUtils.isEmpty(mPassword)) {
			mPasswordView.setError(getString(R.string.error_field_required));
			focusView = mPasswordView;
			cancel = true;
		} else if (mPassword.length() < 4) {
			mPasswordView.setError(getString(R.string.error_invalid_password));
			focusView = mPasswordView;
			cancel = true;
		}

		// Check for a valid email address.
		if (TextUtils.isEmpty(mUser)) {
			mEmailView.setError(getString(R.string.error_field_required));
			focusView = mEmailView;
			cancel = true;
		}

		if (cancel) {
			// There was an error; don't attempt login and focus the first
			// form field with an error.
			focusView.requestFocus();
		} else {
			// Show a progress spinner, and kick off a background task to
			// perform the user login attempt.
			mLoginStatusMessageView.setText(R.string.login_progress_signing_in);
			showProgress(true);
			mAuthTask = new UserLoginTask();
			mAuthTask.execute((Void) null);
		}
	}

	/**
	 * Shows the progress UI and hides the login form.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	private void showProgress(final boolean show) {
		// On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
		// for very easy animations. If available, use these APIs to fade-in
		// the progress spinner.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

			mLoginStatusView.setVisibility(View.VISIBLE);
			mLoginStatusView.animate().setDuration(shortAnimTime).alpha(show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					mLoginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
				}
			});

			mLoginFormView.setVisibility(View.VISIBLE);
			mLoginFormView.animate().setDuration(shortAnimTime).alpha(show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
				}
			});
		} else {
			// The ViewPropertyAnimator APIs are not available, so simply show
			// and hide the relevant UI components.
			mLoginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
			mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
		}
	}

	/**
	 * Represents an asynchronous login/registration task used to authenticate
	 * the user.
	 */
	public class UserLoginTask extends AsyncTask<Void, Void, Intent> {

		@Override
		protected Intent doInBackground(Void... params) {

			if (mToken != null) {
				// just registered, need to check if account active
				try {
					boolean accountActive = webService.accountActive(mToken);

					if (!accountActive) {
						return null;


					}
				} catch (Exception e) {
					VpnStatus.logError(e);
					e.printStackTrace();

				}
				 
			}

			String authtoken = webService.loginForToken(mUser, mPassword);

			final Intent res = new Intent();
			res.putExtra(AccountManager.KEY_ACCOUNT_NAME, mUser);
			res.putExtra(AccountManager.KEY_ACCOUNT_TYPE, AccountGeneral.ACCOUNT_TYPE);
			res.putExtra(AccountManager.KEY_AUTHTOKEN, authtoken);
			res.putExtra(PARAM_USER_PASS, mPassword);
			res.putExtra(ARG_IS_ADDING_NEW_ACCOUNT, true);
			return res;
		}

		@Override
		protected void onPostExecute(Intent intent) {
			if (intent == null) {
				AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
				builder.setMessage(R.string.account_created_succesfully).setCancelable(false)
						.setPositiveButton("OK", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {

							}
						});
				AlertDialog alert = builder.create();
				alert.show();
			} else {
				finishLogin(intent);
			}
			
			
			mAuthTask = null;
			showProgress(false);

		}

		@Override
		protected void onCancelled() {
			mAuthTask = null;
			showProgress(false);
		}
	}

	private void finishLogin(Intent intent) {
		String accountName = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
		String accountPassword = intent.getStringExtra(PARAM_USER_PASS);

		String authtoken = intent.getStringExtra(AccountManager.KEY_AUTHTOKEN);

		if (authtoken == null || authtoken.equals("")) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(getString(R.string.login_error)).setCancelable(false)
					.setPositiveButton("OK", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {

						}
					});
			AlertDialog alert = builder.create();
			alert.show();

			return;
		}

		final Account account = new Account(accountName, intent.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE));
		if (intent.getBooleanExtra(ARG_IS_ADDING_NEW_ACCOUNT, false)) {

			String authtokenType = mAuthTokenType;
			// Creating the account on the device and setting the auth token we
			// got
			// (Not setting the auth token will cause another call to the server
			// to authenticate the user)
			mAccountManager.addAccountExplicitly(account, accountPassword, null);
			mAccountManager.setAuthToken(account, authtokenType, authtoken);
		} else {
			mAccountManager.setPassword(account, accountPassword);
		}
		setAccountAuthenticatorResult(intent.getExtras());
		setResult(RESULT_OK, intent);
		finish();
	}
}
