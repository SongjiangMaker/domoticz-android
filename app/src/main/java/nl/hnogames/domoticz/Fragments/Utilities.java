package nl.hnogames.domoticz.Fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;

import nl.hnogames.domoticz.Adapters.UtilityAdapter;
import nl.hnogames.domoticz.Containers.UtilitiesInfo;
import nl.hnogames.domoticz.Domoticz.Domoticz;
import nl.hnogames.domoticz.Interfaces.DomoticzFragmentListener;
import nl.hnogames.domoticz.Interfaces.UtilitiesReceiver;
import nl.hnogames.domoticz.Interfaces.setCommandReceiver;
import nl.hnogames.domoticz.Interfaces.thermostatClickListener;
import nl.hnogames.domoticz.R;
import nl.hnogames.domoticz.UI.UtilitiesInfoDialog;
import nl.hnogames.domoticz.app.DomoticzFragment;

public class Utilities extends DomoticzFragment implements DomoticzFragmentListener,
        thermostatClickListener {

    private Domoticz mDomoticz;
    private ArrayList<UtilitiesInfo> mUtilitiesInfos;


    private long thermostatSetPointValue;

    private ListView listView;
    private UtilityAdapter adapter;
    private ProgressDialog progressDialog;
    private Activity mActivity;


    @Override
    public void refreshFragment() {
        if(mSwipeRefreshLayout!=null)
            mSwipeRefreshLayout.setRefreshing(true);

        processUtilities();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = activity;
        getActionBar().setTitle(R.string.title_utilities);
    }


    @Override
    public void Filter(String text) {
        try {
            if (adapter != null)
                adapter.getFilter().filter(text);
            super.Filter(text);
        } catch (Exception ex) {
        }
    }

    @Override
    public void onConnectionOk() {
        showProgressDialog();

        mDomoticz = new Domoticz(mActivity);
        processUtilities();
    }

    private void processUtilities()
    {
        final thermostatClickListener listener = this;
        mDomoticz.getUtilities(new UtilitiesReceiver() {

            @Override
            public void onReceiveUtilities(ArrayList<UtilitiesInfo> mUtilitiesInfos) {
                successHandling(mUtilitiesInfos.toString(), false);

                Utilities.this.mUtilitiesInfos = mUtilitiesInfos;
                adapter = new UtilityAdapter(mActivity, mUtilitiesInfos, listener);

                createListView();
                hideProgressDialog();
            }

            @Override
            public void onError(Exception error) {
                errorHandling(error);
            }
        });
    }

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private void createListView() {
        mSwipeRefreshLayout = (SwipeRefreshLayout) getView().findViewById(R.id.swipe_refresh_layout);

        listView = (ListView) getView().findViewById(R.id.listView);
        listView.setAdapter(adapter);
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view,
                                           int index, long id) {
                showInfoDialog(Utilities.this.mUtilitiesInfos.get(index));
                return true;
            }
        });
        mSwipeRefreshLayout.setRefreshing(false);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                processUtilities();
            }
        });
    }

    private void showInfoDialog(final UtilitiesInfo mUtilitiesInfo) {
        UtilitiesInfoDialog infoDialog = new UtilitiesInfoDialog(
                getActivity(),
                mUtilitiesInfo,
                R.layout.dialog_utilities_info);
        infoDialog.setIdx(String.valueOf(mUtilitiesInfo.getIdx()));
        infoDialog.setLastUpdate(mUtilitiesInfo.getLastUpdate());
        infoDialog.setIsFavorite(mUtilitiesInfo.getFavoriteBoolean());
        infoDialog.show();
        infoDialog.onDismissListener(new UtilitiesInfoDialog.DismissListener() {
            @Override
            public void onDismiss(boolean isChanged, boolean isFavorite) {
                if (isChanged) changeFavorite(mUtilitiesInfo, isFavorite);
            }
        });
    }

    private void changeFavorite(final UtilitiesInfo mUtilitiesInfo, final boolean isFavorite) {
        addDebugText("changeFavorite");
        addDebugText("Set idx " + mUtilitiesInfo.getIdx() + " favorite to " + isFavorite);

        int jsonAction;
        int jsonUrl = Domoticz.Json.Url.Set.FAVORITE;

        if (isFavorite) jsonAction = Domoticz.Device.Favorite.ON;
        else jsonAction = Domoticz.Device.Favorite.OFF;

        mDomoticz.setAction(mUtilitiesInfo.getIdx(), jsonUrl, jsonAction, 0, new setCommandReceiver() {
            @Override
            public void onReceiveResult(String result) {
                successHandling(result, false);
                mUtilitiesInfo.setFavoriteBoolean(isFavorite);
            }

            @Override
            public void onError(Exception error) {
                errorHandling(error);
            }
        });
    }


    @Override
    public void onClick(final int idx, int action, long newSetPoint) {
        addDebugText("onClick");
        addDebugText("Set idx " + idx + " to " + String.valueOf(newSetPoint));

        thermostatSetPointValue = newSetPoint;

        int jsonUrl = Domoticz.Json.Url.Set.TEMP;

        mDomoticz.setAction(idx, jsonUrl, action, newSetPoint, new setCommandReceiver() {
            @Override
            public void onReceiveResult(String result) {
                updateThermostatSetPointValue(idx, thermostatSetPointValue);
                successHandling(result, false);
            }

            @Override
            public void onError(Exception error) {
                errorHandling(error);
            }
        });

    }

    /**
     * Updates the set point in the Utilities container
     *
     * @param idx         ID of the utility to be changed
     * @param newSetPoint The new set point value
     */
    private void updateThermostatSetPointValue(int idx, long newSetPoint) {
        addDebugText("updateThermostatSetPointValue");

        for (UtilitiesInfo info : mUtilitiesInfos) {
            if (info.getIdx() == idx) {
                info.setSetPoint(newSetPoint);
                break;
            }
        }
        notifyDataSetChanged();
    }

    /**
     * Notifies the list view adapter the data has changed and refreshes the list view
     */
    private void notifyDataSetChanged() {
        addDebugText("notifyDataSetChanged");
        // adapter.notifyDataSetChanged();
        listView.setAdapter(adapter);

    }

    /**
     * Initializes the progress dialog
     */
    private void initProgressDialog() {
        progressDialog = new ProgressDialog(this.getActivity());
        progressDialog.setMessage(getString(R.string.msg_please_wait));
        progressDialog.setCancelable(false);
    }

    /**
     * Shows the progress dialog if isn't already showing
     */
    private void showProgressDialog() {
        if (progressDialog == null) initProgressDialog();
        if (!progressDialog.isShowing())
            progressDialog.show();
    }

    /**
     * Hides the progress dialog if it is showing
     */
    private void hideProgressDialog() {
        if (progressDialog.isShowing())
            progressDialog.dismiss();
    }

    @Override
    public void errorHandling(Exception error) {
        super.errorHandling(error);
        hideProgressDialog();
    }
}