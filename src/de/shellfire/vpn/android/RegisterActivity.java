package de.shellfire.vpn.android;

import java.math.BigInteger;
import java.security.SecureRandom;

import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import de.shellfire.vpn.android.auth.UserEmailFetcher;
import de.shellfire.vpn.android.auth.VpnAuthenticatorActivity;
import de.shellfire.vpn.android.openvpn.VpnStatus;
import de.shellfire.vpn.android.webservice.ShellfireWebService;
import de.shellfire.vpn.android.webservice.WsRegistrationResult;

public class RegisterActivity extends Activity {

	private TextView mTosTextView;
	private CheckBox mTosCheckBox;
	private TextView mRegisterStatusMessageView;
	private View mRegisterFormView;
	private View mRegisterStatusView;

	private ShellfireWebService webService;
	private String mUser;
	private String mPassword;

	private int mSubscribeToNewsletter;

	public final static String PARAM_USER_PASS = "USER_PASS";
	public final static String PARAM_USER_HAS_LOGIN = "PARAM_USER_HAS_LOGIN";
	
	
	private RegisterTask mRegisterTask;
	private CheckBox mCheckRulesRegsBox;
	private CheckBox mCheckNewsletterBox;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_register);
		
		webService = ShellfireWebService.getInstance();

		mRegisterFormView = findViewById(R.id.textRulesRegs);
		mRegisterStatusView = findViewById(R.id.register_status);
		mRegisterStatusMessageView = (TextView) findViewById(R.id.register_status_message);

		// Set up the register form.
		
		mTosTextView = (TextView) findViewById(R.id.textAcceptRulesRegs);

		String tos = "";
		if (webService.getLang().equals("de")) {
			tos = "Ich akzeptiere die <a target='_agb' href='https://www.shellfire.de/agb/?&utm_nooverride=1'>AGB</a> und habe die <a target='_datenschutzerklaerung' href='https://www.shellfire.de/datenschutzerklaerung/?&utm_nooverride=1'>Datenschutzerklärung</a> sowie das <a target='_widerrufsrecht' href='https://www.shellfire.de/widerrufsrecht/?&utm_nooverride=1'>Widerrufsrecht</a> zur Kenntnis genommen";			
		} else if (webService.getLang().equals("fr")) {
			tos = "J'accepte les <a target='_agb' href='https://www.shellfire.fr/agb/?&utm_nooverride=1'>CGV</a> et j'ai pris connaissance de la <a target='_datenschutzerklaerung' href='https://www.shellfire.fr/datenschutzerklaerung/?&utm_nooverride=1'>déclaration de protection de données</a> et des <a target='_widerrufsrecht' href='https://www.shellfire.fr/widerrufsrecht/?&utm_nooverride=1'>avis de rétractationa</a>";
		} else if (webService.getLang().equals("es")) {
			tos = "Acepto los <a target='_agb' href='https://www.shellfire.net/agb/?&utm_nooverride=1'>Términos y Condiciones</a> y he leído <a target='_datenschutzerklaerung' href='https://www.shellfire.net/datenschutzerklaerung/?&utm_nooverride=1'>la política de privacidad</a> y <a target='_widerrufsrecht' href='https://www.shellfire.net/widerrufsrecht/?&utm_nooverride=1'>el aviso de retiro</a>.";
		} else if (webService.getLang().equals("tr")) {
			tos = "Ben <a target='_agb' href='https://www.shellfire.net/agb/?&utm_nooverride=1'>Şartlar ve Koşullar</a> kabul ediyorum ve <a target='_datenschutzerklaerung' href='https://www.shellfire.net/datenschutzerklaerung/?&utm_nooverride=1'>Gizlilik Politikası</a> ve <a target='_widerrufsrecht' href='https://www.shellfire.net/widerrufsrecht/?&utm_nooverride=1'>Çekilme</a> haber okudum.";
		} else if (webService.getLang().equals("ar")) {
			tos = "أوافق على شروط وأحكام و قرأت سياسة الخصوصية وإشعار الانسحاب: <a target='_agb' href='https://www.shellfire.net/agb/?&utm_nooverride=1'>الشروط والأحكام</AGB>, <a target='_datenschutzerklaerung' href='https://www.shellfire.net/datenschutzerklaerung/?&utm_nooverride=1'>بيان الخصوصية</a>, <a target='_widerrufsrecht' href='https://www.shellfire.net/widerrufsrecht/?&utm_nooverride=1'>انسحاب</a>";
		} else {
			// default to english
			tos = "I accept the <a target='_agb' href='https://www.shellfire.net/agb/?&utm_nooverride=1'>Terms & Conditions</a> and take note of the <a target='_datenschutzerklaerung' href='https://www.shellfire.net/datenschutzerklaerung/?&utm_nooverride=1'>Privacy Statement</a> and the <a target='_widerrufsrecht' href='https://www.shellfire.net/widerrufsrecht/?&utm_nooverride=1'>Right of Withdrawal</a>";
		}
			
		mTosTextView.setText(Html.fromHtml(tos));

		mTosTextView.setMovementMethod(LinkMovementMethod.getInstance());

		mTosCheckBox = (CheckBox) findViewById(R.id.checkRulesRegs);
		mTosTextView.setOnTouchListener(new View.OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {

				// no link was touched, so handle the touch to change
				// the pressed state of the CheckBox
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					CharSequence text = mTosTextView.getText();

					// find out which character was touched
					int offset = mTosTextView.getOffsetForPosition(event.getX(), event.getY());

					// check if this character contains a URL
					URLSpan[] types = ((Spanned) text).getSpans(offset, offset, URLSpan.class);

					if (types.length > 0) {

						String url = types[0].getURL();
						if (url != null) {

							Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
							startActivity(browserIntent);
						}

						return false;
					}

					mTosCheckBox.setPressed(true);
					break;

				case MotionEvent.ACTION_UP:
					mTosCheckBox.setChecked(!mTosCheckBox.isChecked());
					mTosCheckBox.setPressed(false);
					break;

				default:
					mTosCheckBox.setPressed(false);
					break;
				}
				return true;
			}
		});

	}

	public void onClickRegister_button(View view) {
		attempRegister();
	}
	
	
	public void onClickHaveAccount(View view) {
		Intent intent = new Intent();
		intent.putExtra(PARAM_USER_HAS_LOGIN, true);
		setResult(RESULT_OK, intent);
		finish();
	}
	
	

	public void attempRegister() {

		// Store values at the time of the login attempt.
		mUser = UserEmailFetcher.getEmail(this);
		mPassword = new BigInteger(130, new SecureRandom()).toString(32);

		
		mCheckRulesRegsBox = (CheckBox) findViewById(R.id.checkRulesRegs);
		mCheckNewsletterBox = (CheckBox) findViewById(R.id.checkNewsletter);

		mSubscribeToNewsletter = mCheckNewsletterBox.isChecked() ? 1 : 0;

		boolean cancel = false;
		View focusView = null;

		if (!mCheckRulesRegsBox.isChecked()) {
			mCheckRulesRegsBox.setError(getString(R.string.error_field_required));
			focusView = mCheckRulesRegsBox;
			cancel = true;
		}

		if (cancel) {
			// There was an error; don't attempt login and focus the first
			// form field with an error.
			focusView.requestFocus();
		} else {
			// Show a progress spinner, and kick off a background task to
			// perform the user login attempt.
			mRegisterStatusMessageView.setText(R.string.register_process_registering);
			showProgress(true);
			mRegisterTask = new RegisterTask();
			mRegisterTask.execute();
		}

	}

	public class RegisterTask extends AsyncTask<Void, Void, WsRegistrationResult> {

		@Override
		protected WsRegistrationResult doInBackground(Void... params) {

			WsRegistrationResult result;
			try {
				result = webService.registerNewFreeAccountWithGooglePlay(mUser, mPassword, mSubscribeToNewsletter);
				return result;

			} catch (Exception e) {
				VpnStatus.logError(e);
				e.printStackTrace();
			}

			return null;
		}

		@Override
		protected void onPostExecute(WsRegistrationResult result) {
			finishRegistration(result);

			mRegisterTask = null;
			showProgress(false);
		}

		@Override
		protected void onCancelled() {
			mRegisterTask = null;
			showProgress(false);
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

		mRegisterStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
		mRegisterFormView.setVisibility(show ? View.GONE : View.VISIBLE);

	}

	public void finishRegistration(WsRegistrationResult result) {

		if (result != null && result.isRegistrationOk()) {
			Intent intent = new Intent();
			intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, mUser);
			intent.putExtra(PARAM_USER_PASS, mPassword);
			intent.putExtra("token", result.getToken());
			setResult(RESULT_OK, intent);
			finish();
		} else {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			String error = "";
			if (result != null)
				error = result.getErrorMessage();
			
			builder.setTitle("Registrierungsfehler").setMessage(error).setCancelable(false)
					.setPositiveButton("OK", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {

						}
					});
			AlertDialog alert = builder.create();
			alert.show();

			return;

		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.register, menu);

		return true;
	}

}
