package uni.fmi.masters.covid_19statistic.ui.covidStatistics;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class CovidStatisticsViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public CovidStatisticsViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is slideshow fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}