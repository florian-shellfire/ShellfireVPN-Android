package de.shellfire.vpn.android;

import java.io.EOFException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.net.ssl.SSLException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.VpnService;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableLayout.LayoutParams;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import de.shellfire.vpn.android.MainActivity.MainSectionFragment.SimpleConnectionStatus;
import de.shellfire.vpn.android.auth.AccountGeneral;
import de.shellfire.vpn.android.auth.VpnAuthenticatorActivity;
import de.shellfire.vpn.android.billing.IabHelper;
import de.shellfire.vpn.android.billing.IabHelper.OnConsumeFinishedListener;
import de.shellfire.vpn.android.billing.IabHelper.OnIabPurchaseFinishedListener;
import de.shellfire.vpn.android.billing.IabResult;
import de.shellfire.vpn.android.billing.Inventory;
import de.shellfire.vpn.android.billing.Purchase;
import de.shellfire.vpn.android.billing.SkuDetails;
import de.shellfire.vpn.android.openvpn.OpenVPNManagement;
import de.shellfire.vpn.android.openvpn.OpenVpnService;
import de.shellfire.vpn.android.openvpn.VPNLaunchHelper;
import de.shellfire.vpn.android.openvpn.VpnProfile;
import de.shellfire.vpn.android.openvpn.VpnStatus;
import de.shellfire.vpn.android.openvpn.VpnStatus.ConnectionStatus;
import de.shellfire.vpn.android.openvpn.VpnStatus.LogItem;
import de.shellfire.vpn.android.openvpn.VpnStatus.LogListener;
import de.shellfire.vpn.android.openvpn.VpnStatus.StateListener;
import de.shellfire.vpn.android.webservice.Entry;
import de.shellfire.vpn.android.webservice.NotLoggedInException;
import de.shellfire.vpn.android.webservice.ShellfireWebService;
import de.shellfire.vpn.android.webservice.Star;
import de.shellfire.vpn.android.webservice.VpnAttributeContainer;
import de.shellfire.vpn.android.webservice.VpnAttributeElement;
import de.shellfire.vpn.android.webservice.VpnAttributeList;
import de.shellfire.vpn.android.webservice.WsUpgradeResult;

public class MainActivity extends FragmentActivity implements ActionBar.TabListener, OnVpnChangedListener, StateListener {
	private static final String PREMIUM_PLUS = "Premium\nPlus";
	private static final String USER_VPN_PERMISSION = "USER_VPN_PERMISSION";
	private static final String USER_VPN_PERMISSION_CANCELLED = "USER_VPN_PERMISSION_CANCELLED";
	private static final String SELECTED_VPN_ID = "selectedVpnId";
	public static LinkedList<Server> serverList;
	public VpnAttributeList premiumTableInfo;
	public static String packageName;
	
	private void handleException(final Exception e) {
		VpnStatus.logError(e);
		e.printStackTrace();
		final MainActivity mainActivity = this;
		
		if (e instanceof UnknownHostException || e instanceof EOFException || e instanceof SocketTimeoutException || e instanceof SSLException) {
			runOnUiThread(new Runnable() {
				public void run() {
					AlertDialog.Builder alert = new AlertDialog.Builder(mainActivity);
					alert.setTitle(getString(R.string.network_problem_title));
					alert.setMessage(getString(R.string.network_problem_message) + "\n\n" +e.toString());
					alert.setPositiveButton(getString(android.R.string.ok),null);
					alert.show();   			
				}
			});
		}
		
	}
	
	public void onClickRefreshServerLoad(View view) {
		new RefreshServerListTask(this).execute();
	}
	
	class RefreshServerListTask extends AsyncTask<Integer, Void, Object> {

		private ProgressDialog dialog;
		private Activity activity;

		public RefreshServerListTask(Activity activity) {
			this.activity = activity;
			context = activity;
			dialog = new ProgressDialog(context);
		}

		private Context context;
		private AlertDialog networkWarningDialog;

		protected void onPreExecute() {
			this.dialog.setMessage(activity.getString(R.string.loading));
			this.dialog.setIndeterminate(true);
			this.dialog.setCancelable(false);
			this.dialog.show();
		}

		protected Object doInBackground(Integer... params) {

			try {
				if (isInternetAvailable() && networkWarningDialog != null && networkWarningDialog.isShowing()) {
					runOnUiThread(new Runnable() {
						public void run() {
							networkWarningDialog.hide();
						}
					});
				}

				serverList = webService.getServerList(true);


			} catch (NotLoggedInException e) {
				VpnStatus.logError(e);
				checkLoginGoToLoginScreen();
			} catch (Exception e) {
				if (isInternetAvailable()) {
					handleException(e);	
				} else {
					final AlertDialog.Builder alert = new AlertDialog.Builder(activity);
					alert.setTitle(getString(R.string.network_problem_title));
					alert.setMessage(getString(R.string.network_problem_message));
					
					runOnUiThread(new Runnable() {
						public void run() {
							networkWarningDialog = alert.show();
						}
					});
						
						
					while (!isInternetAvailable()) {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
					}
					return doInBackground(params);
				}
			}

			return true;
		}

		protected void onPostExecute(Object various) {
			try {
				if (dialog.isShowing()) {

					dialog.dismiss();
				}
			} catch (Exception e) {
				// nothing
			}

			if (serverList != null) {
				ServerSelectSectionFragment fragment = getFragmentServerSelect();
				if (fragment != null)
					fragment.createList(serverList, null);
				
				getFragmentMain().showVpnAndServer();
			}
		}

	}
	
	
	public class UpgradeVpnTask extends AsyncTask<Integer, Void, WsUpgradeResult> {
		private String signature;
		private String signed_data;
		private ProgressDialog dialog;

		protected void onPreExecute() {

			this.dialog.setMessage(getString(R.string.verifying_purchase));
			this.dialog.setIndeterminate(true);
			this.dialog.setCancelable(false);
			this.dialog.show();
		}

		public UpgradeVpnTask(String signed_data, String signature) {

			this.signed_data = signed_data;
			this.signature = signature;
			this.dialog = new ProgressDialog(MainActivity.this);

		}

		protected WsUpgradeResult doInBackground(Integer... params) {

			try {
				WsUpgradeResult result = webService.upgradeVpnToPremiumWithGooglePlayPurchase(signed_data, signature);

				if (result.getUpgradeSuccesful()) {
					IabHelper helper = getIabHelper();
					if (helper != null) {
						inventory = helper.queryInventory();
					}


					Vpn selectedVpn = webService.getSelectedVpn();
					if (selectedVpn != null) {
						int vpnProductId = selectedVpn.getVpnId();

						webService.getAllVpnDetails(true);
						webService.setSelectedVpn(vpnProductId);
						webService.setSelectedServer(selectedVpn.getServer());

						isPremium = true;
						webService.setIsPremium(true);
						return result;

					}
				}
			} catch (NotLoggedInException e) {
				VpnStatus.logError(e);
				checkLoginGoToLoginScreen();
			} catch (Exception e) {
				handleException(e);
			}

			return null;
		}

		protected void onPostExecute(WsUpgradeResult result) {
			if (dialog.isShowing()) {
				try {
					dialog.dismiss();
				} catch (Exception e) {
					// nothing
				}
			}

			if (result != null && result.getUpgradeSuccesful()) {
				IabHelper helper = getIabHelper();
				if (helper != null) {
					// filter item_ only
					List<Purchase> allPurchases = inventory.getAllPurchases();
					LinkedList<Purchase> toConsume = new LinkedList<Purchase>();

					for (Purchase cur : allPurchases) {
						if (cur.getSku().startsWith("item")) {
							toConsume.add(cur);
						}
					}

					helper.consumeAsync(toConsume, null);
				}

				if (webService.getSelectedVpn() != null) {
					showPurchasedVpnStatus(webService.getSelectedVpn(), null);

					DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							switch (which) {
							case DialogInterface.BUTTON_POSITIVE:
								// switch to premium fragment
								getActionBar().setSelectedNavigationItem(1);
								break;

							case DialogInterface.BUTTON_NEGATIVE:
								// stay on premium info tab, do nothing
								break;
							}
						}
					};
					AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
					builder.setMessage(R.string.premium_upgrade_succesful_show_serverlist).setPositiveButton(R.string.yes, dialogClickListener)
							.setNegativeButton(R.string.no, dialogClickListener).show();

				}

			} else {
				showMessage(getString(R.string.upgrade_not_successful_check_log));
				if (premiumTableInfo != null) {
					showPurchasePremiumTable(premiumTableInfo, null);
				}

			}

		}

	}

	public class UpgradeVpnTaskCobi extends AsyncTask<Integer, Void, WsUpgradeResult> {
		private ProgressDialog dialog;
		private String code;

		protected void onPreExecute() {

			this.dialog.setMessage(getString(R.string.verifying_purchase));
			this.dialog.setIndeterminate(true);
			this.dialog.setCancelable(false);
			this.dialog.show();
		}

		public UpgradeVpnTaskCobi(String code) {
			this.dialog = new ProgressDialog(MainActivity.this);
			this.code = code;
		}

		protected WsUpgradeResult doInBackground(Integer... params) {

			try {
				WsUpgradeResult result = webService.upgradeVpnToPremiumWithCobiCode(code);

				if (result.getUpgradeSuccesful()) {

					Vpn selectedVpn = webService.getSelectedVpn();
					if (selectedVpn != null) {
						int vpnProductId = selectedVpn.getVpnId();

						webService.getAllVpnDetails(true);
						webService.setSelectedVpn(vpnProductId);
						webService.setSelectedServer(selectedVpn.getServer());

						isPremium = true;
						webService.setIsPremium(true);
						

					}
				}
				return result;
			} catch (NotLoggedInException e) {
				VpnStatus.logError(e);
				checkLoginGoToLoginScreen();
			} catch (Exception e) {
				handleException(e);
			}

			return null;
		}

		protected void onPostExecute(WsUpgradeResult result) {
			if (dialog.isShowing()) {
				try {
					dialog.dismiss();
				} catch (Exception e) {
					// nothing
				}
			}

			if (result != null && result.getUpgradeSuccesful()) {

				if (webService.getSelectedVpn() != null) {
					showPurchasedVpnStatus(webService.getSelectedVpn(), null);

					DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							switch (which) {
							case DialogInterface.BUTTON_POSITIVE:
								// switch to premium fragment
								getActionBar().setSelectedNavigationItem(1);
								break;

							case DialogInterface.BUTTON_NEGATIVE:
								// stay on premium info tab, do nothing
								break;
							}
						}
					};
					AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
					builder.setMessage(R.string.premium_upgrade_succesful_show_serverlist).setPositiveButton(R.string.yes, dialogClickListener)
							.setNegativeButton(R.string.no, dialogClickListener).show();

				}

			} else {
				AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
				String msg = getString(R.string.upgrade_not_successful);
				if (result != null) {
					msg += ": " + result.getError();
				}
				
				builder.setMessage(msg)
					.setNeutralButton(android.R.string.ok, null)
					.show();

				if (premiumTableInfo != null) {
					showPurchasePremiumTable(premiumTableInfo, null);
				}

			}

		}

	}

	private static final int LOGIN = 1;
	private static final int SELECT_VPN = 2;
	private static final int SHOW_PREFERENCES = 3;
	private static final int LOGOUT = 4;

	private static final int REQUEST_PURCHASE = 124;
	private static final int START_VPN_PROFILE = 70;
	private static ShellfireWebService webService;

	String TAG = "MainActivity";

	private AccountManager mAccountManager;

	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide
	 * fragments for each of the sections. We use a
	 * {@link android.support.v4.app.FragmentStatePagerAdapter} derivative,
	 * which will keep every loaded fragment in memory. If this becomes too
	 * memory intensive, it may be best to switch to a
	 * {@link android.support.v4.app.FragmentStatePagerAdapter}.
	 */
	SectionsPagerAdapter mSectionsPagerAdapter;
	private int mPageCount;

	@Override
	public void onDestroy() {
		super.onDestroy();

		if (mIabHelper != null) {
			try {
				mIabHelper.dispose();
			} catch (IllegalArgumentException ex) {
				ex.printStackTrace();
			} finally {
			}
		}
		mIabHelper = null;

		VpnStatus.removeStateListener(this);

	}

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	static ViewPager mViewPager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		packageName = getApplicationContext().getPackageName();

		mAccountManager = AccountManager.get(this);

		// compute your public key and store it in base64EncodedPublicKey
		mIabHelper = new IabHelper(this);

		mIabHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
			public void onIabSetupFinished(IabResult result) {
				if (!result.isSuccess()) {
					// Oh noes, there was a problem.
					Log.d(TAG, getString(R.string.problem_setting_up_in_app_billing) + result);
				} else {
					// Hooray, IAB is fully set up!
					
				}

			}
		});

		if (VpnPreferences.getShowLog(this)) {
			mPageCount = 4;
		} else {
			mPageCount = 3;
		}

		// Set up the action bar.
		final ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		webService = ShellfireWebService.getInstance();

		mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager(), mPageCount);
		// Create the adapter that will return a fragment for each of the three
		// primary sections of the app.

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);

		// When swiping between different sections, select the corresponding
		// tab. We can also use ActionBar.Tab#select() to do this if we have
		// a reference to the Tab.
		mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				actionBar.setSelectedNavigationItem(position);

			}
		});

		// For each of the sections in the app, add a tab to the action bar.
		for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
			// Create a tab with text corresponding to the page title defined by
			// the adapter. Also specify this Activity object, which implements
			// the TabListener interface, as the callback (listener) for when
			// this tab is selected.
			actionBar.addTab(actionBar.newTab().setText(mSectionsPagerAdapter.getPageTitle(i)).setTabListener(this));

		}

		VpnStatus.addStateListener(this);

		Runnable action = new Runnable() {
			public void run() {
				if (webService.isLoggedIn()) {
				} else {
					checkLoginGoToLoginScreen();
				}
			}
		};
		
		if (isInternetAvailable()) {
			action.run();
			// try to get account frmo account manager
		} else {
			showDialogInternetRequired(R.string.retry, action);
		}
		
		
	}

	private void checkLoginGoToLoginScreen() {
		final Account availableAccounts[] = mAccountManager.getAccountsByType(AccountGeneral.ACCOUNT_TYPE);
		if (availableAccounts.length == 0) {
			startLoginActivity();

		} else {
			getExistingAccountAuthToken(availableAccounts[0], AccountGeneral.AUTHTOKEN_TYPE_USE_VPN);
		}
	}
	
	
	
	class InitializeEverythingTask extends AsyncTask<Integer, Void, Object> {

		private ProgressDialog dialog;
		private Activity activity;

		public InitializeEverythingTask(Activity activity) {
			this.activity = activity;
			context = activity;
			dialog = new ProgressDialog(context);
		}

		private Context context;
		private AlertDialog networkWarningDialog;
		private boolean displayNewVersionAvailable;
		

		protected void onPreExecute() {
			this.dialog.setMessage(activity.getString(R.string.loading));
			this.dialog.setIndeterminate(true);
			this.dialog.setCancelable(false);
			this.dialog.show();
		}

		protected Object doInBackground(Integer... params) {

			try {
				if (isInternetAvailable() && networkWarningDialog != null && networkWarningDialog.isShowing()) {
					runOnUiThread(new Runnable() {
						public void run() {
							networkWarningDialog.hide();
						}
					});
				}

				premiumTableInfo = webService.getComparisonTableData();
				LinkedList<Vpn> vpnList = webService.getAllVpnDetails(false);
				webService.loadSkuList();
				webService.getDeveloperPayload();
				serverList = webService.getServerList();

				isPremium = webService.isPremium();
				
				int currentVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
				int mostRecentVersion = webService.getMostRecentVersion();
				if (mostRecentVersion > currentVersion) {
					displayNewVersionAvailable = true;
				}
				

				// decide if we need the vpn selection activity / intent
				boolean doSelectIntent = true;

				if (vpnList != null && vpnList.size() > 1 && !VpnPreferences.getRememberVpnSelection(MainActivity.this)) {
					return doSelectIntent;
				}

				Vpn vpn = null;
				if (VpnPreferences.getRememberVpnSelection(MainActivity.this)) {
					int vpnId = VpnPreferences.getRememberedVpnSelection(MainActivity.this);

					if (vpnId > 0) {
						if (vpnList != null) {
							for (Vpn curVpn : vpnList) {
								if (curVpn.getVpnId() == vpnId) {
									vpn = curVpn;
									break;
								}
							}

						}

					}
				}

				if (vpnList == null || vpnList.size() == 0) {
					vpn = webService.createNewFreeVpn();
				} else if (vpnList.size() == 1) {
					vpn = vpnList.get(0);
				}

				if (vpn != null) {
					webService.setSelectedVpn(vpn.getVpnId());
				}

				// check if active subscriptions or items are available
				
				webService.loadSkuList();
				
				List<String> itemSkuList = webService.getItemSkuList();
				List<String> subSkuList = webService.getSubSkuList();
				
				IabHelper helper = getIabHelper();
				if (helper != null) {
					inventory = helper.queryInventory(true, itemSkuList, subSkuList);
					for (String sku : inventory.getAllOwnedSkus()) {
						Purchase p = inventory.getPurchase(sku);
						try {
							webService.upgradeVpnToPremiumWithGooglePlayPurchase(p.getOriginalJson(), p.getSignature());
						} catch (Exception e) {

						}
					}
				}

				List<String> ownedSkus = inventory.getAllOwnedSkus();
				if (ownedSkus != null && ownedSkus.size() > 0) {

					boolean hasPremium = false;
					// we own a sku - check if we have a premium vpn

					vpnList = webService.getAllVpnDetails(false);
					if (vpnList != null && vpnList.size() > 0) {
						for (Vpn curVpn : vpnList) {
							if (curVpn.getAccountType() != ServerType.Free) {
								hasPremium = true;
							}
						}
					}

					if (hasPremium) {
						// we own a product, i tried to activate it but it seems
						// that even though i do not have
						// a premium vpn - i need to consume all the owned SKU

						for (String curSku : ownedSkus) {
							final Purchase toConsume = inventory.getPurchase(curSku);

							// only consume non-subscriptions
							if (toConsume.getSku().startsWith("item")) {
								runOnUiThread(new Runnable() {
									public void run() {
										IabHelper iabHelper = getIabHelper(); 
										if (iabHelper != null) {
											iabHelper.consumeAsync(toConsume, new OnConsumeFinishedListener() {
												@Override
												public void onConsumeFinished(Purchase purchase, IabResult result) {
													showMessage(context.getString(R.string.item_consumed));
												}
											});
										}
									}
								});
							}
						}
					}
				}

				if (vpn != null) {
					webService.setSelectedServer(vpn.getServer());
					webService.getParametersForOpenVpn();
					webService.getCertificatesForOpenVpn();

					return vpn;
				}

			} catch (NotLoggedInException e) {
				VpnStatus.logError(e);
				checkLoginGoToLoginScreen();
			} catch (Exception e) {
				if (isInternetAvailable()) {
					handleException(e);	
				} else {
					final AlertDialog.Builder alert = new AlertDialog.Builder(activity);
					alert.setTitle(getString(R.string.network_problem_title));
					alert.setMessage(getString(R.string.network_problem_message));
					
					runOnUiThread(new Runnable() {
						public void run() {
							networkWarningDialog = alert.show();
						}
					});
						
						
					while (!isInternetAvailable()) {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
					}
					return doInBackground(params);
				}
				
				
				
				
			}

			return true;
		}

		protected void onPostExecute(Object various) {
			try {
				if (dialog.isShowing()) {

					dialog.dismiss();
				}
			} catch (Exception e) {
				// nothing
			}

			if (serverList != null) {
				ServerSelectSectionFragment fragment = getFragmentServerSelect();
				if (fragment != null)
					fragment.createList(serverList, null);

			}

			boolean doSelectIntent;
			boolean showMainScreen = false;
			if (various instanceof Vpn) {
				MainSectionFragment fragment = getFragmentMain();
				if (fragment != null)
					fragment.showVpnAndServer();

				showMainScreen = true;
			} else if (various instanceof Boolean) {
				doSelectIntent = (Boolean) various;
				if (doSelectIntent) {
					Intent intent = new Intent(MainActivity.this, SelectVpnActivity.class);
					startActivityForResult(intent, SELECT_VPN);
				} else {
					showMainScreen = true;
				}

			} else {
				showMainScreen = true;
			}

			if (showMainScreen) {
				View mainlayout = findViewById(R.id.mainLayout);
				if (mainlayout == null)
					mainlayout = activity.findViewById(R.id.mainLayout);

				if (mainlayout != null) {
					mainlayout.setVisibility(android.view.View.VISIBLE);
				}

				everythingLoaded = true;
			}
			
			
			if (this.displayNewVersionAvailable && !VpnPreferences.getIgnoreNewVersionWarning(MainActivity.this)) {
				AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
				alert.setTitle(getString(R.string.new_version_available_title));
				alert.setMessage(getString(R.string.new_version_available_message));
				alert.setPositiveButton(android.R.string.ok,new OnClickListener() {
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
					    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=de.shellfire.vpn.android")));
					}		
				});
				alert.setNegativeButton(R.string.dontshowagain,new OnClickListener() {
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						VpnPreferences.setIgnoreNewVersionWarning(MainActivity.this, true);
					}		
				});
				alert.setNeutralButton(android.R.string.no,null);
				
				alert.setCancelable(true);
				alert.show();  		
				
			}

		}

	}

	public static Inventory inventory;

	private void startLoginActivity() {
		final Runnable action = new Runnable() {
			public void run() {
				Intent intent = new Intent(getBaseContext(), VpnAuthenticatorActivity.class);
				startActivityForResult(intent, LOGIN);
			}
		};
		
		if (isInternetAvailable()) {
			action.run();
		} else {
			showDialogInternetRequired(R.string.retry, action);
		}
	}

	/**
	 * Get the auth token for an existing account on the AccountManager
	 * 
	 * @param account
	 * @param authTokenType
	 */
	private void getExistingAccountAuthToken(Account account, String authTokenType) {
		final AccountManagerFuture<Bundle> future = mAccountManager.getAuthToken(account, authTokenType, null, this, null, null);

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Bundle bnd = future.getResult();

					final String authtoken = bnd.getString(AccountManager.KEY_AUTHTOKEN);

					synchronized (webService) {
						webService.getComparisonTableData();

						webService.loginWithToken(authtoken);
						if (webService.isLoggedIn()) {
							afterLoginOk();

						} else {
							startLoginActivity();
						}
					}

				} catch (Exception e) {
					handleException(e);
				}

			}

		}).start();
	}

	private void showMessage(final String msg) {
		if (TextUtils.isEmpty(msg))
			return;

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT).show();
			}
		});
	}

	private void afterLoginOk() {
		// only if inflation already sucesful
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				final Runnable action = new Runnable() {
					public void run() {
						new InitializeEverythingTask(MainActivity.this).execute();
					}
				};
				
				if (isInternetAvailable()) {
					action.run();
				} else {
					showDialogInternetRequired(R.string.retry, action);
				}
			}
		});
	}

	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		// Check which request we're responding to
		if (requestCode == LOGIN) {
			// Make sure the request was successful
			if (resultCode == RESULT_OK) {
				afterLoginOk();

			} else {
				Intent intent = new Intent(this, VpnAuthenticatorActivity.class);
				startActivityForResult(intent, LOGIN);
			}
		}
		if (requestCode == SELECT_VPN) {
			if (resultCode == RESULT_OK) {
				Bundle extras = data.getExtras();
				final int vpnId = extras.getInt(SELECTED_VPN_ID);

				Runnable action = new Runnable() {
					public void run() {
						registerSelectedVpn(vpnId);
					}
				};
				
				if (isInternetAvailable()) {
					action.run();
				} else {
					showDialogInternetRequired(R.string.retry, action);
				}
				
			}
		}

		if (requestCode == START_VPN_PROFILE) {
			if (resultCode == Activity.RESULT_OK) {
				setMainFragmentSimpleConnectionStatus(SimpleConnectionStatus.Connecting);
				new StartOpenVpnThread().start();
			} else if (resultCode == Activity.RESULT_CANCELED) {
				setMainFragmentSimpleConnectionStatus(SimpleConnectionStatus.Disconnected);
				// User does not want us to start, so we just vanish
				VpnStatus.updateStateString(USER_VPN_PERMISSION_CANCELLED, "", R.string.state_user_vpn_permission_cancelled,
						ConnectionStatus.LEVEL_NOTCONNECTED);
			}
		}
		if (requestCode == SHOW_PREFERENCES) {
			loadPref();
		}
		if (requestCode == REQUEST_PURCHASE) {
			new AsyncTask<Integer, Void, Void>() {
				protected Void doInBackground(Integer... params) {

					IabHelper iabHelper = getIabHelper();
					if (iabHelper != null) {
						iabHelper.handleActivityResult(requestCode, resultCode, data);	
					}

					return null;
				}
			}.execute();

		}

	}

	private void loadPref() {
		// hier kann man geänderte einstellungen nach der rückkehr vom settings
		// dialog aktualisieren!
	}

	private void registerSelectedVpn(int vpnId) {
		new RegisterSelectedVpnTask(this).execute(vpnId);
	}

	class RegisterSelectedVpnTask extends AsyncTask<Integer, Void, Boolean> {

		private ProgressDialog dialog;
		private Context context;

		public RegisterSelectedVpnTask(Context context) {
			this.context = context;

			dialog = new ProgressDialog(context);
		}

		protected void onPreExecute() {
			this.dialog.setMessage(context.getString(R.string.selecting_vpn));
			this.dialog.setIndeterminate(true);
			this.dialog.setCancelable(false);
			this.dialog.show();
		}

		protected Boolean doInBackground(Integer... params) {
			int vpnId = params[0];
			synchronized (webService) {
				try {
					webService.setSelectedVpn(vpnId);
					Vpn vpn = webService.getSelectedVpn();
					if (vpn == null) {
						VpnPreferences.setRememberedVpnSelection(getBaseContext(), null);

						LinkedList<Vpn> vpnList = webService.getAllVpnDetails(false);
						if (vpnList.size() == 1) {
							vpn = vpnList.get(0);
							webService.setSelectedVpn(vpnId);
							webService.setSelectedServer(vpn.getServer());
							return true;
						} else {
							return false;
						}

					} else {
						webService.setSelectedServer(vpn.getServer());
						return true;
					}

				} catch (NotLoggedInException e) {
					VpnStatus.logError(e);
					checkLoginGoToLoginScreen();
				} catch (Exception e) {
					handleException(e);
				}

			}

			return false;
		}

		protected void onPostExecute(Boolean selectionOk) {
			try {
				dialog.dismiss();
			} catch (Exception e) {
				// nothing
			}

			if (selectionOk) {
				MainSectionFragment mainFrag = getFragmentMain();
				if (mainFrag != null) {
					mainFrag.showVpnAndServer();
				}

				View mainlayout = findViewById(R.id.mainLayout);

				if (mainlayout != null)
					mainlayout.setVisibility(android.view.View.VISIBLE);

				everythingLoaded = true;
			} else {
				// registration of remembered vpn did not work, go to
				// VpnSelectionTask
				// or try to pick the 1 vpn that is in the list if we have one
				Intent intent = new Intent(getBaseContext(), SelectVpnActivity.class);
				startActivityForResult(intent, SELECT_VPN);
			}

		}

	}

	MainSectionFragment getFragmentMain() {
		if (mViewPager != null)
			return (MainSectionFragment) mSectionsPagerAdapter.getRegisteredFragment(0);
		else
			return null;
	}

	ServerSelectSectionFragment getFragmentServerSelect() {
		if (mViewPager != null)
			return (ServerSelectSectionFragment) mSectionsPagerAdapter.getRegisteredFragment(1);
		else
			return null;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);

		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		Account[] accounts;
		switch (item.getItemId()) {
		case R.id.action_settings:
			Intent prefIntent = new Intent();
			prefIntent.setClass(MainActivity.this, SetPreferenceActivity.class);
			startActivityForResult(prefIntent, SHOW_PREFERENCES);

			break;
		case R.id.action_about:
			Intent intent = new Intent(this, AboutActivity.class);
			startActivity(intent);

			break;
		case R.id.action_rate:
			new AppRater(this).rateAppNow();
			
			break;
		case R.id.action_share:
			Intent sendIntent = new Intent();
			sendIntent.setAction(Intent.ACTION_SEND);
			sendIntent.putExtra(Intent.EXTRA_TEXT, getResources().getText(R.string.share_text));
			sendIntent.setType("text/plain");
			startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.share_with)));
			
			break;
		}

		return true;
	}

	@Override
	public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
	}

	@Override
	public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
	}

	SparseArray<Fragment> fragmentMap = new SparseArray<Fragment>();

	public static boolean everythingLoaded;

	/**
	 * A {@link FragmentStatePagerAdapter} that returns a fragment corresponding
	 * to one of the sections/tabs/pages.
	 */
	public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

		private int mPageCount;

		public SectionsPagerAdapter(FragmentManager fm, int numPages) {
			super(fm);
			mPageCount = numPages;
		}

		@Override
		public Fragment getItem(int position) {
			// getItem is called to instantiate the fragment for the given page.
			if (fragmentMap.get(position) == null) {
				if (position == 0) {
					Fragment fragment = new MainSectionFragment();
					fragmentMap.put(position, fragment);
				} else if (position == 1) {
					Fragment fragment = new ServerSelectSectionFragment();
					fragmentMap.put(position, fragment);
				} else if (position == 2) {
					Fragment fragment = new BillingFragment();
					fragmentMap.put(position, fragment);
				} else {

					LogSectionFragment fragment = new LogSectionFragment();

					VpnStatus.addStateListener(fragment);
					VpnStatus.addLogListener(fragment);
					Bundle args = new Bundle();
					args.putInt(LogSectionFragment.ARG_SECTION_NUMBER, position + 1);
					fragment.setArguments(args);
					fragmentMap.put(position, fragment);
				}

			}

			return fragmentMap.get(position);
		}

		SparseArray<Fragment> registeredFragments = new SparseArray<Fragment>();

		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			Fragment fragment = (Fragment) super.instantiateItem(container, position);
			registeredFragments.put(position, fragment);
			return fragment;
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			registeredFragments.remove(position);
			super.destroyItem(container, position, object);
		}

		public Fragment getRegisteredFragment(int position) {
			return registeredFragments.get(position);
		}

		@Override
		public int getCount() {
			return mPageCount;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			Locale l = Locale.getDefault();
			switch (position) {
			case 0:
				return getString(R.string.title_connect).toUpperCase(l);
			case 1:
				return getString(R.string.title_serverlist).toUpperCase(l);
			case 2:
				return getString(R.string.title_billing).toUpperCase(l);
			case 3:
				return getString(R.string.title_log).toUpperCase(l);
			}
			return null;
		}
	}

	private static StringBuffer logBuffer = new StringBuffer();

	/**
	 * A dummy fragment representing a section of the app, but that simply
	 * displays dummy text.
	 */
	public static class LogSectionFragment extends Fragment implements StateListener, LogListener {
		/**
		 * The fragment argument representing the section number for this
		 * fragment.
		 */
		public static final String ARG_SECTION_NUMBER = "section_number";
		private TextView appLogTextView;

		public static Handler uiHandler;

		static {
			uiHandler = new Handler(Looper.getMainLooper());
		}

		public LogSectionFragment() {
			logBuffer = new StringBuffer();
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_log, container, false);
			appLogTextView = (TextView) rootView.findViewById(R.id.textViewAppLog);
			setRetainInstance(true);

			if (logBuffer != null) {
				appLogTextView.append(logBuffer.toString());
			}

			
			return rootView;
		}

		@Override
		public void updateState(String state, String logmessage, int localizedResId, ConnectionStatus level) {
			final StringBuffer msg = new StringBuffer();
			msg.append("--------------------\n");
			msg.append("State Change\n");
			msg.append("Time: " + SimpleDateFormat.getTimeInstance().format(new Date()) + "\n");
			msg.append("State: " + state + "\n");
			msg.append("Logmessage: " + logmessage + "\n");
			msg.append("--------------------\n\n");

			logBuffer.append(msg.toString());
			if (appLogTextView != null) {
				uiHandler.post(new Runnable() {

					@Override
					public void run() {
						appLogTextView.append(msg.toString());
					}
				});
			}
		}

		@Override
		public void newLog(LogItem logItem) {
			final StringBuffer msg = new StringBuffer();
			msg.append("--------------------\n");
			msg.append("New Log\n");
			msg.append("Time: " + SimpleDateFormat.getTimeInstance().format(new Date()) + "\n");
			FragmentActivity a = getActivity();
			String s = "";
			if (a != null)
				s = logItem.getString(a);

			msg.append("Logmessage: " + s + "\n");
			msg.append("--------------------\n\n");

			logBuffer.append(msg.toString());
			if (appLogTextView != null) {
				uiHandler.post(new Runnable() {

					@Override
					public void run() {
						appLogTextView.append(msg.toString());
					}
				});
			}
		}

	}

	public void onClickSendLogToShellfire(View view) {
		final MainActivity activity = this;
		Runnable action = new Runnable() {
			@Override
			public void run() {
				new SendLogTask(activity).execute();					
			}
		};
		
		if (isInternetAvailable()) {
			action.run();
		} else {
			showDialogInternetRequired(R.string.retry, action);
		}
	}

	class SendLogTask extends AsyncTask<Void, Void, Boolean> {
		private ProgressDialog dialog;
		private Activity context;
		private String logString;

		public SendLogTask(Activity activity) {
			context = activity;
			dialog = new ProgressDialog(context);
			logString = logBuffer.toString();
		}

		protected void onPreExecute() {
			this.dialog.setMessage(context.getString(R.string.send_log_to_shellfire));
			this.dialog.setIndeterminate(true);
			this.dialog.setCancelable(false);
			this.dialog.show();
		}

		@Override
		protected Boolean doInBackground(Void... arg0) {
			try {
				boolean result = webService.sendLogToShellfire(logString);
				return result;
			} catch (Exception e) {
				handleException(e);
			}

			return false;
		}

		protected void onPostExecute(Boolean result) {
			try {
				dialog.dismiss();
			} catch (Exception e) {
				// nothing
			}

			if (result) {
				Toast.makeText(context, R.string.log_send_succesfully, Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(context, R.string.log_could_not_be_sent, Toast.LENGTH_SHORT).show();
			}
		}

	}

	void setVpnValues(View rowView, Vpn vpn) {
		// assign vpn name
		if (rowView != null) {
			TextView nameView = (TextView) rowView.findViewById(R.id.vpnName);
			nameView.setText(vpn.getName().trim());

			// assign vpnAccountType (Free, Premium, PremiumPlus) + stars
			TextView vpnAccountTypeView = (TextView) rowView.findViewById(R.id.vpnAccountType);
			
			int accountTypeResId = Util.getServerTypeResId(vpn.getAccountType());
			vpnAccountTypeView.setText(accountTypeResId);

			int numStars;
			if (vpn.getAccountType() == ServerType.Free)
				numStars = 1;
			else if (vpn.getAccountType() == ServerType.Premium)
				numStars = 3;
			else
				numStars = 5;

			for (int i = 5 - numStars; i >= 1; i--) {
				int id;
				if (i == 1)
					id = R.id.star_1;
				else if (i == 2)
					id = R.id.star_2;
				else if (i == 3)
					id = R.id.star_3;
				else if (i == 4)
					id = R.id.star_4;
				else
					id = R.id.star_5;

				ImageView starView = (ImageView) rowView.findViewById(id);
				starView.setVisibility(android.view.View.INVISIBLE);
			}

		}

	}

	private int getCountryImageResId(Country country) {
		String isoCode = CountryMap.get(country);

		int resId = getResources().getIdentifier(isoCode, "drawable", getPackageName());

		return resId;
	}
	
	private int getCountryNameResId(Country country) {
		int resId = getResources().getIdentifier(country.toString().toLowerCase() , "string",  getPackageName());

		return resId;
	}		
		

	/**
	 * A dummy fragment representing a section of the app, but that simply
	 * displays dummy text.
	 */
	public static class MainSectionFragment extends Fragment {
		private static final String SIMPLE_CONNECTION_STATUS = "SimpleConnectionStatus";
		/**
		 * The fragment argument representing the section number for this
		 * fragment.
		 */
		public static final String ARG_USER_NAME = "user_name";
		TextView userTextView;

		private SimpleConnectionStatus connectionStatus;
		private MainActivity mainActivity;
		private Button buttonConnect;
		private TextView stateView;

		public void onAttach(Activity activity) {
			super.onAttach(activity);
			this.mainActivity = (MainActivity) activity;

		}

		public void showVpnAndServer() {
			if (getView() != null) {
				showVpnAndServer(getView());
			}
		}

		public void showVpnAndServer(View view) {
			synchronized (webService) {
				Vpn selectedVpn = webService.getSelectedVpn();
				if (selectedVpn != null) {
					View progressBarVpn = view.findViewById(R.id.progressBarVpn);
					progressBarVpn.setVisibility(View.GONE);

					View rowViewVpn = view.findViewById(R.id.includeViewSelectedVpn);
					mainActivity.setVpnValues(rowViewVpn, selectedVpn);
					progressBarVpn.setVisibility(View.GONE);
					rowViewVpn.setVisibility(View.VISIBLE);
				}

				Server selectedServer = webService.getSelectedServer();
				if (selectedServer != null) {
					View progressBarServer = view.findViewById(R.id.progressBarServer);
					progressBarServer.setVisibility(View.GONE);
					View rowViewServer = view.findViewById(R.id.includeViewSelectedServer);
					setServerValues(rowViewServer, selectedServer);
					progressBarServer.setVisibility(View.GONE);
					rowViewServer.setVisibility(View.VISIBLE);
				}
				if (selectedVpn != null && selectedServer == null) {

				}

			}
		}

		void setServerValues(View rowView, Server server) {
			// assign country-image
			int countryResId = mainActivity.getCountryImageResId(server.getCountry());
			TextView countryImageView = (TextView) rowView.findViewById(R.id.country);
			countryImageView.setBackgroundResource(countryResId);
			
			countryImageView.setText(getText(R.string.server) + " "+server.getServerId());
			
			
			// assign name
			TextView nameView = (TextView) rowView.findViewById(R.id.vpnName);
			int countryNameResId = mainActivity.getCountryNameResId(server.getCountry());
			if (countryNameResId != 0)
				nameView.setText(countryNameResId);
			else
				nameView.setText(""+server.getCountry());


			// assign server type
			TextView serverTypeView = (TextView) rowView.findViewById(R.id.vpnAccountType);
			int accountTypeResId = Util.getServerTypeResId(server.getServerType());
			serverTypeView.setText(accountTypeResId);

			int numStars;
			if (server.getServerType() == ServerType.Free)
				numStars = 1;
			else if (server.getServerType() == ServerType.Premium)
				numStars = 3;
			else
				numStars = 5;

			for (int i = 5; i >= 1; i--) {
				int id;
				if (i == 1)
					id = R.id.star_1;
				else if (i == 2)
					id = R.id.star_2;
				else if (i == 3)
					id = R.id.star_3;
				else if (i == 4)
					id = R.id.star_4;
				else
					id = R.id.star_5;

				ImageView starView = (ImageView) rowView.findViewById(id);

				if (i > numStars) {
					starView.setVisibility(android.view.View.GONE);
				} else {
					starView.setVisibility(android.view.View.VISIBLE);

				}
			}

			// assign speed (incl. stars)
			TextView speedTextView = (TextView) rowView.findViewById(R.id.speedTextView);
			speedTextView.setText(server.getServerSpeed().getResId());
			TextView securityTextView = (TextView) rowView.findViewById(R.id.securityTextView);
			securityTextView.setText(server.getSecurity().getResId());

			// assign loadPercentage
			int load = server.getLoadPercentage();
			ProgressBar loadBar = (ProgressBar) rowView.findViewById(R.id.loadBar);
			loadBar.setProgress(load);

			TextView progressText = (TextView) rowView.findViewById(R.id.progressText);
			progressText.setText(load + " %");
			
		}

		public void onCreate(Bundle save) {
			super.onCreate(save);
			setRetainInstance(true);
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container, false);

			if (savedInstanceState != null) {
				this.connectionStatus = (SimpleConnectionStatus) savedInstanceState.getSerializable(SIMPLE_CONNECTION_STATUS);
			}

			View mainlayout = rootView.findViewById(R.id.mainLayout);
			if (everythingLoaded) {
				mainlayout.setVisibility(android.view.View.VISIBLE);
			} else {
				mainlayout.setVisibility(android.view.View.INVISIBLE);
			}

			buttonConnect = (Button) rootView.findViewById(R.id.buttonConnect);
			stateView = (TextView) rootView.findViewById(R.id.connectionState);

			if (VpnStatus.getLastState() == ConnectionStatus.LEVEL_CONNECTED) {
				setSimpleConnectionStatus(SimpleConnectionStatus.Connected);
			}

			showVpnAndServer(rootView);
			setSimpleConnectionStatus(this.connectionStatus);
			return rootView;
		}

		public void setSimpleConnectionStatus(final SimpleConnectionStatus conStatus) {

			this.connectionStatus = conStatus;

			if (conStatus == SimpleConnectionStatus.Connecting) {
				buttonConnect.setText(R.string.connecting);
				buttonConnect.setEnabled(false);
				stateView.setText(R.string.connecting);
				stateView.setTextColor(getResources().getColor(R.color.connecting));

			} else if (conStatus == SimpleConnectionStatus.Connected) {
				buttonConnect.setText(R.string.disconnect);
				buttonConnect.setEnabled(true);
				stateView.setText(R.string.connected);
				stateView.setTextColor(getResources().getColor(R.color.connected));
			} else if (conStatus == SimpleConnectionStatus.Disconnected) {
				buttonConnect.setText(R.string.connect);
				buttonConnect.setEnabled(true);
				stateView.setText(R.string.disconnected);
				stateView.setTextColor(getResources().getColor(R.color.not_connected));
			}

		}

		public void onSaveInstanceState(Bundle outState) {
			outState.putSerializable(SIMPLE_CONNECTION_STATUS, connectionStatus);
		}

		public enum SimpleConnectionStatus {
			Connected, Disconnected, Connecting;
		}

	}

	public void connectParamsLoaded() {

		try {
			mSelectedProfile = new VpnProfile(webService.getParametersForOpenVpn(), webService.getCertificatesForOpenVpn());
		} catch (NotLoggedInException e) {
			VpnStatus.logError(e);
			checkLoginGoToLoginScreen();
		} catch (Exception e) {
			handleException(e);
		}

		Intent intent = VpnService.prepare(MainActivity.this);
		if (intent == null) {
			onActivityResult(START_VPN_PROFILE, Activity.RESULT_OK, null);
		} else {
			VpnStatus.updateStateString(USER_VPN_PERMISSION, "", R.string.state_user_vpn_password, ConnectionStatus.LEVEL_WAITING_FOR_USER_INPUT);

			try {
				startActivityForResult(intent, START_VPN_PROFILE);
			} catch (ActivityNotFoundException ane) {
				VpnStatus.logError(R.string.no_vpn_support_image);
			}
		}

		reconnectPlanned = false;
		numReconnectsPlanned = 0;
	}

	class ConnectTask extends AsyncTask<Void, Void, Void> {
		private ProgressDialog dialog;
		private Activity context;

		public ConnectTask(Activity activity) {
			context = activity;
			dialog = new ProgressDialog(context);
		}

		protected void onPreExecute() {
			this.dialog.setMessage(context.getString(R.string.loading_certificates));
			this.dialog.setIndeterminate(true);
			this.dialog.setCancelable(false);
			this.dialog.show();
		}

		@Override
		protected Void doInBackground(Void... arg0) {
			try {
				webService.getAllVpnDetails(true);
				webService.setSelectedVpn(webService.getSelectedVpn().getVpnId());
				webService.setSelectedServer(webService.getSelectedVpn().getServer());
				webService.getCertificatesForOpenVpn();
				webService.getParametersForOpenVpn();
				return null;
			} catch (NotLoggedInException e) {
				VpnStatus.logError(e);
				checkLoginGoToLoginScreen();
			} catch (Exception e) {
				handleException(e);
			}

			return null;
		}

		protected void onPostExecute(Void zoid) {
			try {
				dialog.dismiss();
			} catch (Exception e) {
				// nothing
			}

			connectParamsLoaded();
		}
	}

	private void disconnect() {
		OpenVpnService service = OpenVpnService.getInstance();
		if (service != null) {
			OpenVPNManagement management = service.getManagement();

			if (management != null) {
				management.stopVPN();
			}
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
	
	public void onClickConnect(View view) {
		if (connectionStatus == SimpleConnectionStatus.Disconnected) {
			final MainActivity activity = this;
			Runnable action = new Runnable() {
				@Override
				public void run() {
					if (webService.getParamsLoaded()) {
						connectParamsLoaded();
					} else {
						new ConnectTask(activity).execute();
					}					
				}
			};
			
			if (isInternetAvailable()) {
				action.run();
			} else {
				showDialogInternetRequired(R.string.retry, action);
			}
			

		} else {
			disconnect();
			new AppRater(this).show();
		}

	}

	private ProgressDialog progress;

	private VpnProfile mSelectedProfile;

	private static SimpleConnectionStatus connectionStatus;
	private static boolean reconnectPlanned = false;
	private static int numReconnectsPlanned = 0;

	private IabHelper mIabHelper;
	public Boolean isPremium = null;

	private class StartOpenVpnThread extends Thread {

		@Override
		public void run() {

			VPNLaunchHelper.startOpenVpn(mSelectedProfile, getBaseContext());

		}

	}

	private void setMainFragmentSimpleConnectionStatus(SimpleConnectionStatus status) {
		MainSectionFragment mainFragment = getFragmentMain();
		connectionStatus = status;
		if (mainFragment != null && mainFragment.getView() != null)
			mainFragment.setSimpleConnectionStatus(status);
	}

	@Override
	public void onVpnChanged() {
		getFragmentMain().showVpnAndServer();

	}

	public void onClickBuyDayPremium(View view) {
		new StartPurchaseFlowTask(this, false, ServerType.Premium, 1).execute();
	}

	public void onClickBuyDayPremiumPlus(View view) {
		new StartPurchaseFlowTask(this, false, ServerType.PremiumPlus, 1).execute();
	}

	public void onClickBuyMonthPremium(View view) {
		new StartPurchaseFlowTask(this, false, ServerType.Premium, 30).execute();
	}

	public void onClickBuyMonthPremiumPlus(View view) {
		new StartPurchaseFlowTask(this, false, ServerType.PremiumPlus, 30).execute();
	}

	public void onClickBuyMonthlyPremium(View view) {
		new StartPurchaseFlowTask(this, true, ServerType.Premium, 1).execute();
	}

	public void onClickCobiCampaign(View view) {
		final Activity activity = this;
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		LayoutInflater inflater = getLayoutInflater();
		final View layoutView = inflater.inflate(R.layout.dialog_cobi, null);
		builder.setView(layoutView);

		final EditText et1 = (EditText) layoutView.findViewById(R.id.et_cb_1);
		final EditText et2 = (EditText) layoutView.findViewById(R.id.et_cb_2);
		final EditText et3 = (EditText) layoutView.findViewById(R.id.et_cb_3);

		builder.setMessage(R.string.enter_cobi_code);
		builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {

				String code = et1.getText() + "-" + et2.getText() + "-" + et3.getText();

				new UpgradeVpnTaskCobi(code).execute();

			}
		});
		builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				// User cancelled the dialog
			}
		});

		final AlertDialog dialog;
		dialog = builder.create();
		if (dialog != null) {

			dialog.show();

			if (et1 != null) {
				et1.addTextChangedListener(new TextWatcher() {
					public void onTextChanged(CharSequence s, int start, int before, int count) {
						if (et1.getText().toString().length() == 2) {
							et2.requestFocus();
						}
					}

					public void beforeTextChanged(CharSequence s, int start, int count, int after) {
					}

					public void afterTextChanged(Editable s) {
					}
				});

				et1.setOnFocusChangeListener(new View.OnFocusChangeListener() {
					@Override
					public void onFocusChange(View view, boolean focused) {
						if (focused) {
							dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
						}
					}
				});
			}
			if (et2 != null) {
				et2.addTextChangedListener(new TextWatcher() {
					public void onTextChanged(CharSequence s, int start, int before, int count) {
						if (et2.getText().toString().length() == 4) {
							et3.requestFocus();
						}
					}

					public void beforeTextChanged(CharSequence s, int start, int count, int after) {
					}

					public void afterTextChanged(Editable s) {
					}
				});
			}

			if (et1 != null) {
				et1.setFocusable(true);
				et1.requestFocus();
			}
		}

	}

	public void onClickBuyMonthlyPremiumPlus(View view) {
		new StartPurchaseFlowTask(this, true, ServerType.PremiumPlus, 1).execute();
	}

	public void onClickBuyYearlyPremium(View view) {
		new StartPurchaseFlowTask(this, true, ServerType.Premium, 12).execute();
	}

	public void onClickBuyYearlyPremiumPlus(View view) {
		new StartPurchaseFlowTask(this, true, ServerType.PremiumPlus, 12).execute();
	}

	class StartPurchaseFlowTask extends AsyncTask<Void, Void, Void> {

		private Activity context;
		private int billingPeriod;
		private ServerType accountType;
		private boolean isSubscription;

		public StartPurchaseFlowTask(Activity activity, boolean isSubscription, ServerType accountType, int billingPeriod) {

			context = activity;

			this.accountType = accountType;
			this.billingPeriod = billingPeriod;
			this.isSubscription = isSubscription;
		}

		protected Void doInBackground(Void... zoid) {
			try {
				String sku = webService.getSku(isSubscription, accountType, billingPeriod);
				IabHelper iabHelper = getIabHelper();

				OnIabPurchaseFinishedListener listener = new OnIabPurchaseFinishedListener() {

					@Override
					public void onIabPurchaseFinished(IabResult result, final Purchase info) {
						if (result != null) {
							if (result.getResponse() == IabHelper.BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED) {
								/*
								 * showMessage(context.getString(R.string.
								 * item_already_owned_consuming)); List<String>
								 * skuList = new LinkedList<String>(); try {
								 * skuList
								 * .add(webService.getSku(ServerType.Premium,
								 * 1)); inventory =
								 * getIabHelper().queryInventory(true, skuList,
								 * skuList); final Purchase toConsume =
								 * inventory.getPurchase(skuList.get(0));
								 * 
								 * runOnUiThread(new Runnable() { public void
								 * run() {
								 * getIabHelper().consumeAsync(toConsume, new
								 * OnConsumeFinishedListener() {
								 * 
								 * @Override public void
								 * onConsumeFinished(Purchase purchase,
								 * IabResult result) {
								 * showMessage(context.getString
								 * (R.string.item_consumed)); } }); } });
								 * 
								 * } catch (NotLoggedInException e) {
								 * VpnStatus.logError(e);
								 * checkLoginGoToLoginScreen(); } catch
								 * (Exception e) { VpnStatus.logError(e);
								 * e.printStackTrace(); }
								 */

							}
						}

						if (info != null) {
							final String signed_data = info.getOriginalJson();
							final String signature = info.getSignature();

							runOnUiThread(new Runnable() {

								@Override
								public void run() {
									new UpgradeVpnTask(signed_data, signature).execute();
								}

							});

						} else {
							runOnUiThread(new Runnable() {

								@Override
								public void run() {
									new UpgradeVpnTask(null, null).execute();
								}

							});
						}

					}

				};

				String payload = webService.getDeveloperPayload();

				if (iabHelper != null) {
					if (isSubscription) {
						iabHelper.launchSubscriptionPurchaseFlow(context, sku, REQUEST_PURCHASE, listener, payload);
					} else {
						iabHelper.launchPurchaseFlow(context, sku, REQUEST_PURCHASE, listener, payload);
					}
				}

				// iabHelper.launchPurchaseFlow(context, sku, REQUEST_PURCHASE,
				// listener, "myData");

			} catch (NotLoggedInException e) {
				VpnStatus.logError(e);
				checkLoginGoToLoginScreen();
			} catch (Exception e) {
				handleException(e);
			}

			return null;
		}

		protected void onPostExecute(Void... zoid) {
			/*
			 * if (dialog.isShowing()) { dialog.dismiss(); }
			 */
		}
	}

	public void addContainerToTable(TableLayout table, VpnAttributeContainer container, boolean first) {
		// add header row

		TableRow headerRow = new TableRow(this);
		headerRow.setId(currentIdCounter++);
		headerRow.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		// headerRow.setBackgroundColor(getResources().getColor(R.color.vpntable_header_bg));

		String name = container.getContainerName();
		TextView view = new TextView(this);
		// view.setBackgroundColor(getResources().getColor(R.color.vpntable_header_bg));
		view.setTextColor(getResources().getColor(R.color.vpntable_header_text));
		view.setText(name);
		
		headerRow.addView(view);

		TextView free = new TextView(this, null);
		// free.setBackgroundColor(getResources().getColor(R.color.vpntable_header_bg));
		free.setTextColor(getResources().getColor(R.color.vpntable_header_text));
		free.setGravity(Gravity.CENTER);

		headerRow.addView(free);

		TextView premium = new TextView(this, null);
		// premium.setBackgroundColor(getResources().getColor(R.color.vpntable_header_bg));
		premium.setTextColor(getResources().getColor(R.color.vpntable_header_text));
		premium.setGravity(Gravity.CENTER);

		headerRow.addView(premium);

		TextView pp = new TextView(this, null);
		// pp.setBackgroundColor(getResources().getColor(R.color.vpntable_header_bg));
		pp.setTextColor(getResources().getColor(R.color.vpntable_header_text));
		pp.setGravity(Gravity.CENTER);

		headerRow.addView(pp);

		if (first) {
			free.setText(Util.getServerTypeResId(ServerType.Free));
			premium.setText(Util.getServerTypeResId(ServerType.Premium));
			String text = (String) getText(Util.getServerTypeResId(ServerType.PremiumPlus));
			pp.setText(text.replace(" ", "\n"));
		}

		table.addView(headerRow);

		int i = 0;
		for (VpnAttributeElement element : container.getElements()) {
			TableRow row = getRowForAttributeElement(element, i++ % 2 == 0);

			table.addView(row);
		}

	}

	private TableRow getRowForAttributeElement(VpnAttributeElement element, boolean even) {

		TableRow elementRow = new TableRow(this);
		elementRow.setId(currentIdCounter++);

		elementRow.setLayoutParams(new LayoutParams(0, LayoutParams.WRAP_CONTENT));
		elementRow.setGravity(Gravity.CENTER_VERTICAL);

		String name = element.getName();

		// write name to first cell
		TextView nameView = new TextView(this);
		nameView.setTextColor(getResources().getColor(R.color.vpntable_row_text));
		nameView.setGravity(Gravity.RIGHT);

		nameView.setText(name + " ");
		elementRow.addView(nameView);

		// write free to 2nd cell
		Entry free = element.getFree();
		View freeCell = getCellForEntry(free, even);
		elementRow.addView(freeCell);

		// write premium to 3rd cell
		Entry premium = element.getPremium();
		View premiumCell = getCellForEntry(premium, even);
		elementRow.addView(premiumCell);

		// write pp to 4th cell
		Entry pp = element.getPp();
		View ppCell = getCellForEntry(pp, even);
		elementRow.addView(ppCell);

		if (even) {
			elementRow.setBackgroundColor(getResources().getColor(R.color.vpntable_row_even_bg));
		} else {
			elementRow.setBackgroundColor(getResources().getColor(R.color.vpntable_row_uneven_bg));
		}

		return elementRow;
	}

	private View getCellForEntry(Entry entry, boolean even) {
		View result = null;

		if (entry.isBoolEntry()) {
			result = getEntry(entry.isBool(), even);
		} else if (entry.isStringEntry()) {
			result = getEntry(entry.getText(), even);
		} else if (entry.isStarEntry()) {
			result = getEntry(entry.getStar(), even);
		}

		android.widget.TableRow.LayoutParams p = new android.widget.TableRow.LayoutParams();
		p.rightMargin = dpToPixel(3, this); // right-margin = 10dp
		p.leftMargin = dpToPixel(3, this); // right-margin = 10dp
		result.setLayoutParams(p);

		return result;
	}

	private static Float scale;

	public static int dpToPixel(int dp, Context context) {
		if (scale == null)
			scale = context.getResources().getDisplayMetrics().density;
		return (int) ((float) dp * scale);
	}

	private LinearLayout getEntry(Star star, boolean even) {
		LinearLayout l = new LinearLayout(this);
		l.setOrientation(LinearLayout.VERTICAL);
		if (even) {
			l.setBackgroundColor(getResources().getColor(R.color.vpntable_row_even_bg));
		} else {
			l.setBackgroundColor(getResources().getColor(R.color.vpntable_row_uneven_bg));
		}

		LinearLayout starLayout = new LinearLayout(this);
		starLayout.setOrientation(LinearLayout.HORIZONTAL);
		starLayout.setGravity(Gravity.CENTER_HORIZONTAL);

		for (int i = 1; i <= star.getNumStars(); i++) {
			ImageView aStar = new ImageView(this);
			aStar.setImageDrawable(getResources().getDrawable(R.drawable.star_green_small));
			starLayout.addView(aStar);
		}

		l.addView(starLayout);

		TextView view = new TextView(this);
		view.setText(star.getText());
		view.setGravity(Gravity.CENTER_HORIZONTAL);
		view.setTextColor(getResources().getColor(R.color.vpntable_row_text));

		l.addView(view);

		return l;
	}

	private TextView getEntry(String text, boolean even) {
		TextView view = new TextView(this);
		view.setText(text);
		view.setGravity(Gravity.CENTER_HORIZONTAL);
		if (even) {
			view.setBackgroundColor(getResources().getColor(R.color.vpntable_row_even_bg));
		} else {
			view.setBackgroundColor(getResources().getColor(R.color.vpntable_row_uneven_bg));
		}
		view.setTextColor(getResources().getColor(R.color.vpntable_row_text));

		return view;
	}

	private ImageView getEntry(boolean bool, boolean even) {
		ImageView view = new ImageView(this);

		if (bool) {
			view.setImageDrawable(getResources().getDrawable(R.drawable.yes));
		} else {
			view.setImageDrawable(getResources().getDrawable(R.drawable.not));
		}

		if (even) {
			view.setBackgroundColor(getResources().getColor(R.color.vpntable_row_even_bg));
		} else {
			view.setBackgroundColor(getResources().getColor(R.color.vpntable_row_uneven_bg));
		}

		return view;
	}

	private int currentIdCounter = 105430;

	public static class BillingFragment extends Fragment {

		private MainActivity mainActivity;

		@Override
		public void onAttach(Activity activity) {
			super.onAttach(activity);
			this.mainActivity = (MainActivity) activity;
			setRetainInstance(true);
		}

		public interface PremiumStateRetrievalFinishedListener {
			public void onPremiumStateRetrievalFinished(Boolean isPremium);
		}

		public interface ComparisonTableRetrievalFinishedListener {
			public void onComparisonTableRetrievalFinished(VpnAttributeList table);
		}

		public void setPremiumState(Boolean isPremium, View rootView) {
			mainActivity.isPremium = isPremium;

			if (mainActivity.isPremium == true) {
				if (webService.getSelectedVpn() != null) {
					mainActivity.showPurchasedVpnStatus(webService.getSelectedVpn(), rootView);
				}
			} else {
				if (mainActivity.premiumTableInfo != null) {
					mainActivity.showPurchasePremiumTable(mainActivity.premiumTableInfo, rootView);
				}
			}
		}

		public void setComparisonTable(VpnAttributeList table, View rootView) {
			mainActivity.premiumTableInfo = table;

			setPremiumState(mainActivity.isPremium, rootView);
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_billing, container, false);

			if (mainActivity.isPremium == null) {
				PremiumStateRetrievalFinishedListener ps = new PremiumStateRetrievalFinishedListener() {
					public void onPremiumStateRetrievalFinished(Boolean isPremium) {
						setPremiumState(isPremium, null);
					}
				};

				webService.isPremiumAsync(ps);
			}

			if (mainActivity.premiumTableInfo == null) {
				try {
					ComparisonTableRetrievalFinishedListener ct = new ComparisonTableRetrievalFinishedListener() {
						public void onComparisonTableRetrievalFinished(VpnAttributeList table) {
							setComparisonTable(table, null);
						}
					};

					webService.getComparisonTableDataAsync(ct);
				} catch (Exception e) {
					mainActivity.handleException(e);
				}
			}

			if (mainActivity.isPremium != null) {
				setPremiumState(mainActivity.isPremium, rootView);
			}

			return rootView;
		}

	}

	public static class ServerSelectSectionFragment extends Fragment {
		public static final String ARG_USER_NAME = "user_name";
		TextView userTextView;
		String mUser;
		ProgressDialog progress;
		private OnVpnChangedListener mCallback;

		@Override
		public void onAttach(Activity activity) {
			super.onAttach(activity);


			// This makes sure that the container activity has implemented
			// the callback interface. If not, it throws an exception
			try {
				mCallback = (OnVpnChangedListener) activity;
			} catch (ClassCastException e) {
				throw new ClassCastException(activity.toString() + " must implement OnVpnChangedListener");
			}
		}

		public void setUser(String user) {
			mUser = user;

		}


		
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			final View rootView = inflater.inflate(R.layout.activity_serverselect_view, container, false);

			createList(serverList, rootView);

			return rootView;
		}

		private void createList(LinkedList<Server> list, View rootView) {
			if (list != null) {
				ListView listview = null;

				if (rootView == null) {
					FragmentActivity a = getActivity();
					if (a != null) {
						listview = (ListView) a.findViewById(R.id.listview);
					}

				} else {
					listview = (ListView) rootView.findViewById(R.id.listview);
				}

				if (listview != null) {
					final ServerListAdapter adapter;
					final FragmentActivity a = getActivity();
					if (a != null) {
						adapter = new ServerListAdapter(a, android.R.layout.simple_list_item_1, list);
						listview.setAdapter(adapter);
					}

					listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

						@Override
						public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
							if (parent != null) {
								final Server item = (Server) parent.getItemAtPosition(position);
								if (item != null) {
									if (view != null) {
										view.setAlpha(1);

										// check if user is eligible to use selected
										// server
										Vpn selectedVpn = webService.getSelectedVpn();
										if (selectedVpn != null) {
											final Server selectedServer = webService.getSelectedServer();

											if (selectedServer != null && selectedServer.getServerId() == item.getServerId()) {
												// already on this server - do nothing

												Toast.makeText(a, R.string.already_on_this_server_doing_nothing, Toast.LENGTH_SHORT).show();

											} else {
												ServerType vpnLevel = selectedVpn.getAccountType();
												ServerType serverLevel = item.getServerType();

												int vpnLevelInt = vpnLevel.ordinal();
												int serverLevelInt = serverLevel.ordinal();

												if (serverLevelInt > vpnLevelInt) {

													DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
														@Override
														public void onClick(DialogInterface dialog, int which) {
															switch (which) {
															case DialogInterface.BUTTON_POSITIVE:
																// switch to premium fragment
																a.getActionBar().setSelectedNavigationItem(2);
																break;

															case DialogInterface.BUTTON_NEGATIVE:
																Toast.makeText(a, R.string.not_showing_premium_info, Toast.LENGTH_LONG).show();

																break;
															}
														}
													};
													AlertDialog.Builder builder = new AlertDialog.Builder(getView().getContext());
													builder.setMessage(R.string.you_are_not_eligible_to_use_this_server).setPositiveButton(R.string.yes, dialogClickListener)
															.setNegativeButton(R.string.no, dialogClickListener).show();

												} else {
													DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
														@Override
														public void onClick(DialogInterface dialog, int which) {
															switch (which) {
															case DialogInterface.BUTTON_POSITIVE:
																// check if connected - if
																// not
																// connected, just change
																// the
																// server. if connected, ask
																// again
																// to doublecheck
																boolean connected = connectionStatus == SimpleConnectionStatus.Connected;

																if (connected) {
																	DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
																		@Override
																		public void onClick(DialogInterface dialog, int which) {
																			switch (which) {
																			case DialogInterface.BUTTON_POSITIVE:
																				OpenVpnService service = OpenVpnService.getInstance();
																				OpenVPNManagement management = service.getManagement();
																				management.stopVPN();
																				changeServerConfirmed(item);

																				break;

																			case DialogInterface.BUTTON_NEGATIVE:
																				break;
																			}
																		}

																	};

																	AlertDialog.Builder builder = new AlertDialog.Builder(getView().getContext());
																	builder.setMessage(R.string.you_are_currently_connecting_disconnect)
																			.setPositiveButton(R.string.yes, dialogClickListener)
																			.setNegativeButton(R.string.no, dialogClickListener).show();

																} else {
																	changeServerConfirmed(item);
																}

																break;

															case DialogInterface.BUTTON_NEGATIVE:
																Toast.makeText(a, R.string.not_changing_server, Toast.LENGTH_LONG).show();

																break;
															}
														}
													};
													AlertDialog.Builder builder = new AlertDialog.Builder(getView().getContext());
													builder.setMessage(R.string.sure_to_change_server).setPositiveButton(R.string.yes, dialogClickListener)
															.setNegativeButton(R.string.no, dialogClickListener).show();

												}

											}											
										}

									}
									
								}
								
								
								
							}
							


						}

					});
				}

			}

		}

		private void changeServerConfirmed(Server item) {
			FragmentActivity a = getActivity();
			if (a != null) {
				new ChangeServerTask((MainActivity) a).execute(item);
			}

		}

		class ChangeServerTask extends AsyncTask<Server, Void, Boolean> {
			private ProgressDialog dialog;
			private MainActivity activity;

			public ChangeServerTask(MainActivity activity) {
				this.activity = activity;
				context = activity;
				dialog = new ProgressDialog(context);
			}

			private Context context;

			protected void onPreExecute() {
				this.dialog.setMessage(activity.getString(R.string.serverchange_is_being_processed_might_take_a_while));
				this.dialog.setIndeterminate(true);
				this.dialog.setCancelable(false);
				this.dialog.show();
			}

			@Override
			protected Boolean doInBackground(Server... params) {
				boolean result = false;
				synchronized (webService) {
					Server serverToSelect = params[0];
					try {
						result = webService.changeServerTo(serverToSelect);
						if (result) {
							webService.getCertificatesForOpenVpn();
							webService.getParametersForOpenVpn();
						}

					} catch (NotLoggedInException e) {
						VpnStatus.logError(e);
						activity.checkLoginGoToLoginScreen();
						return false;
					} catch (Exception e) {
						activity.handleException(e);
						return false;
					}

				}

				return result;
			}

			protected void onPostExecute(Boolean result) {
				try {
					dialog.dismiss();
				} catch (Exception e) {
					// nothing
				}

				if (result) {
					FragmentActivity a = getActivity();
					if (a != null) {
						Toast.makeText(a, R.string.server_selection_succesfully_performed_on_backend, Toast.LENGTH_LONG).show();
					}
					mCallback.onVpnChanged();
					createList(serverList, null);
				} else {
					FragmentActivity a = getActivity();
					if (a != null) {
						Toast.makeText(a, R.string.server_selection_could_not_be_changed, Toast.LENGTH_LONG).show();
					}

				}
			}
		}
	}

	@Override
	public void updateState(final String state, final String logmessage, final int localizedResId, ConnectionStatus level) {

		switch (level) {
		// connecting
		case LEVEL_CONNECTING_NO_SERVER_REPLY_YET:
		case LEVEL_CONNECTING_SERVER_REPLIED:
		case LEVEL_WAITING_FOR_USER_INPUT:
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					// detect a reconnect
					if (connectionStatus == SimpleConnectionStatus.Connected && numReconnectsPlanned < 10) {
						reconnectPlanned = true;
						numReconnectsPlanned++;
						disconnect();
						connectParamsLoaded();
					}

					setMainFragmentSimpleConnectionStatus(SimpleConnectionStatus.Connecting);
					if (progress != null && progress.isShowing()) {
						progress.setMessage(getString(VpnStatus.getLocalizedState(state)));
					} else {
						progress = ProgressDialog.show(MainActivity.this, getString(R.string.connecting_please_wait),
								getString(VpnStatus.getLocalizedState(state)), true);
					}
				}
			});

			break;

		// connected
		case LEVEL_CONNECTED:
			// change buttons and icns and make the app look secured!
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					setMainFragmentSimpleConnectionStatus(SimpleConnectionStatus.Connected);
					if (progress != null && progress.isShowing())
						progress.dismiss();
				}
			});
			break;

		// disconnected
		case LEVEL_NOTCONNECTED:
			// reset buttons and icons back to disconnected mode

			// update only if not reconnect "planned"
			if (!reconnectPlanned) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						setMainFragmentSimpleConnectionStatus(SimpleConnectionStatus.Disconnected);
						if (progress != null && progress.isShowing())
							progress.dismiss();
					}
				});
			}

			break;
		// tbd
		case LEVEL_VPNPAUSED:
		case LEVEL_NONETWORK:
		case LEVEL_AUTH_FAILED:
			webService.clearParamsAndCerts();
			break;
		case UNKNOWN_LEVEL:

		}

	}

	public IabHelper getIabHelper() {
		return mIabHelper;
	}

	private void showPurchasedVpnStatus(Vpn selectedVpn, View rootView) {

		View rowViewVpn = null;
		TextView premiumUntilView = null;
		TextView premiumTypeView = null;

		if (rootView == null) {
			rowViewVpn = findViewById(R.id.includeViewSelectedVpnPremium);
			premiumUntilView = (TextView) findViewById(R.id.textViewPremiumUntil);
			premiumTypeView = (TextView) findViewById(R.id.textViewPremiumType);

			View prem = findViewById(R.id.premiumAccountShowInfo);
			if (prem != null)
				prem.setVisibility(View.VISIBLE);

			View free = findViewById(R.id.freeVpnPresentPremium);
			if (free != null)
				free.setVisibility(View.GONE);

		} else {
			rowViewVpn = rootView.findViewById(R.id.includeViewSelectedVpnPremium);
			premiumUntilView = (TextView) rootView.findViewById(R.id.textViewPremiumUntil);
			premiumTypeView = (TextView) rootView.findViewById(R.id.textViewPremiumType);
			rootView.findViewById(R.id.premiumAccountShowInfo).setVisibility(View.VISIBLE);
			rootView.findViewById(R.id.freeVpnPresentPremium).setVisibility(View.GONE);

		}
		if (rowViewVpn != null) {
			setVpnValues(rowViewVpn, selectedVpn);
			rowViewVpn.setVisibility(android.view.View.VISIBLE);
		}

		ServerType premiumType = selectedVpn.getAccountType();
		if (premiumTypeView != null & premiumType != null) {
			premiumTypeView.setText(premiumType.toString());
		}

		Date premiumUntilDate = selectedVpn.getPremiumUntil();
		if (premiumUntilView != null && premiumUntilDate != null) {
			java.text.DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(MainActivity.this);
			java.text.DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(MainActivity.this);
			String premUntilDateTime = dateFormat.format(premiumUntilDate) + " " + timeFormat.format(premiumUntilDate);

			premiumUntilView.setText(premUntilDateTime);
		}
	}

	private void showPurchasePremiumTable(VpnAttributeList vpnAttributeList, View rootView) {
		View prem = null;
		View free = null;
		TableLayout table = null;

		Button  buttonItemDayPremium = null;
		Button buttonItemDayPremiumPlus = null;
		
		Button buttonItemMonthPremium = null;
		Button buttonItemMonthPremiumPlus = null;
		
		Button buttonSubMonthlyPremium = null;
		Button buttonSubMonthlyPremiumPlus = null;
		
		Button buttonSubYearlyPremium = null;
		Button buttonSubYearlyPremiumPlus = null;
		
		if (rootView == null) {
			prem = findViewById(R.id.premiumAccountShowInfo);
			free = findViewById(R.id.freeVpnPresentPremium);
			table = (TableLayout) findViewById(R.id.premiumInfoTable);
			
			buttonItemDayPremium = (Button)findViewById(R.id.buttonBuyDayPremium);
			buttonItemDayPremiumPlus = (Button)findViewById(R.id.buttonBuyDayPremiumPlus);
			buttonItemMonthPremium = (Button)findViewById(R.id.buttonBuyMonthPremium);
			buttonItemMonthPremiumPlus = (Button)findViewById(R.id.buttonBuyMonthPremiumPlus);
			buttonSubMonthlyPremium = (Button)findViewById(R.id.buttonBuyMonthlyPremium);
			buttonSubMonthlyPremiumPlus = (Button)findViewById(R.id.buttonBuyMonthlyPremiumPlus);
			buttonSubYearlyPremium = (Button)findViewById(R.id.buttonBuyYearlyPremium);
			buttonSubYearlyPremiumPlus = (Button)findViewById(R.id.buttonBuyYearlyPremium);
			
		} else {
			prem = rootView.findViewById(R.id.premiumAccountShowInfo);
			free = rootView.findViewById(R.id.freeVpnPresentPremium);
			table = (TableLayout) rootView.findViewById(R.id.premiumInfoTable);

			buttonItemDayPremium = (Button) rootView.findViewById(R.id.buttonBuyDayPremium);
			buttonItemDayPremiumPlus = (Button) rootView.findViewById(R.id.buttonBuyDayPremiumPlus);
			buttonItemMonthPremium = (Button) rootView.findViewById(R.id.buttonBuyMonthPremium);
			buttonItemMonthPremiumPlus = (Button) rootView.findViewById(R.id.buttonBuyMonthPremiumPlus);
			buttonSubMonthlyPremium = (Button) rootView.findViewById(R.id.buttonBuyMonthlyPremium);
			buttonSubMonthlyPremiumPlus = (Button) rootView.findViewById(R.id.buttonBuyMonthlyPremiumPlus);
			buttonSubYearlyPremium = (Button) rootView.findViewById(R.id.buttonBuyYearlyPremium);
			buttonSubYearlyPremiumPlus = (Button) rootView.findViewById(R.id.buttonBuyYearlyPremiumPlus);
		}
		
		if (prem != null) {
			prem.setVisibility(View.GONE);
		}		
		if (free != null) {
			free.setVisibility(View.VISIBLE);
			try {
				String sku = null;
				SkuDetails skuDetails = null;
				View button = null;
				// Assign user's currency to purchase buttons
				
				// ---- items ----
				// -- 24 hours ---
				// Premium
				sku = webService.getSku(false, ServerType.Premium, 1);
				if (sku != null) {
					skuDetails = inventory.getSkuDetails(sku);
					if (skuDetails != null && buttonItemDayPremium != null ) {
						String price = skuDetails.getPrice();
						buttonItemDayPremium.setText(price);
					}
				}
				
				// Premium Plus
				sku = webService.getSku(false, ServerType.PremiumPlus, 1);
				if (sku != null) {
					skuDetails = inventory.getSkuDetails(sku);
					if (skuDetails != null && buttonItemDayPremiumPlus != null ) {
						String price = skuDetails.getPrice();
						buttonItemDayPremiumPlus.setText(price);
					}
				}
				
				
				// -- 1 month  ---
				// Premium
				sku = webService.getSku(false, ServerType.Premium, 30);
				if (sku != null) {
					skuDetails = inventory.getSkuDetails(sku);
					if (skuDetails != null && buttonItemMonthPremium != null ) {
						String price = skuDetails.getPrice();
						buttonItemMonthPremium.setText(price);
					}
				}
				
				// Premium Plus
				sku = webService.getSku(false, ServerType.PremiumPlus, 30);
				if (sku != null) {
					skuDetails = inventory.getSkuDetails(sku);
					if (skuDetails != null && buttonItemMonthPremiumPlus != null ) {
						String price = skuDetails.getPrice();
						buttonItemMonthPremiumPlus.setText(price);
					}
				}
				
				// ---- subscriptions -----
				// -- monthly  ---
				// Premium
				sku = webService.getSku(true, ServerType.Premium, 1);
				if (sku != null) {
					skuDetails = inventory.getSkuDetails(sku);
					if (skuDetails != null && buttonSubMonthlyPremium != null ) {
						String price = skuDetails.getPrice();
						buttonSubMonthlyPremium.setText(price);
					}
				}

				// Premium Plus
				sku = webService.getSku(true, ServerType.PremiumPlus, 1);
				if (sku != null) {
					skuDetails = inventory.getSkuDetails(sku);
					if (skuDetails != null && buttonSubMonthlyPremiumPlus != null ) {
						String price = skuDetails.getPrice();
						buttonSubMonthlyPremiumPlus.setText(price);
					}
				}
				
				// -- yearly ---
				// Premium
				sku = webService.getSku(true, ServerType.Premium, 12);
				if (sku != null) {
					skuDetails = inventory.getSkuDetails(sku);
					if (skuDetails != null && buttonSubYearlyPremium != null ) {
						String price = skuDetails.getPrice();
						buttonSubYearlyPremium.setText(price);
					}
				}
				
				// Premium Plus
				sku = webService.getSku(true, ServerType.PremiumPlus, 12);
				if (sku != null) {
					skuDetails = inventory.getSkuDetails(sku);
					if (skuDetails != null && buttonSubYearlyPremiumPlus != null ) {
						String price = skuDetails.getPrice();
						buttonSubYearlyPremiumPlus.setText(price);
					}
				}
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			
		}
		


		if (table != null) {
			// draw table
			boolean first = true;
			int i = 0;
			

			
			VpnAttributeContainer[] containers = vpnAttributeList.getContainers();
			for (VpnAttributeContainer container : containers) {

				// only show the first 2 containers in mobile version!
				if (++i > 2)
					continue;

				addContainerToTable(table, container, first);
				first = false;
			}
		}

	}

	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		mViewPager.setCurrentItem(tab.getPosition());

	}
	
	private boolean isInternetAvailable() {
		ConnectivityManager cm =
		        (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
		 
		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		boolean isConnected = activeNetwork != null &&
		                      activeNetwork.isConnectedOrConnecting();
		
		return isConnected;
	}

}
