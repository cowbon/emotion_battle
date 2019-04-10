# Emotion Battle
## Contributor
Howard Weng, Tom Wang
## Description
Compare how strong your rmotion is with your competitor. It also tell your face apart from other players.
This app requires Microsoft Azure web services
## How to run
* Obtain keys from [Microsoft Azure Cognitive Service](https://azure.microsoft.com/en-us/services/cognitive-services/)
	* Face API
	* Emotion API
* Fill them into ```app/src/main/res/values/strings.xml```
```xml=
<resources>
    <string name="app_name">Emotion Battle</string>

    <!-- Emotion subscription key -->
    <!-- Please refer to https://www.projectoxford.ai/ to get your subscription key -->
    <!-- If you have one subscription key, you can add it here to use the service -->
    <string name="emotionSubscription_key">Put Emotion API key here</string>

    <!-- Face subscription key -->
    <!-- Please refer to http://oxford-portal.azurewebsites.net/doc/general/subscription-key-mgmt to get your subscription key -->
    <!-- Please note that the Emotion API and Face API have different subscription keys -->
    <!-- You will need Face subscription key, besides the Emotion subscription keys, for the second part of the sample to work -->
    <string name="faceSubscription_key">Put Face API here</string>
```
* Build & run!!!
## Changelog
```
	v6.0 : A bunch of updates, add face recognition
	v5.1 : Fix camera bug on Android Nougat(7.0)
	v5.0 : 將結果視窗修改成ScrollView，若選擇的照片包含不只一人，會顯示全部的得分(但順序不一定)，可藉由滑
	動來看每個人的得分，但player1與player2分數的分數比較只會比較最後一位的得分~
	v4.3 : 修正部分文字錯誤
	v4.2 : 更改icon
	v4.1 : 將部分顯示換成 ScrollView，使訊息能夠全部顯示，且不超出畫面
	v4.0 : 新增使用者提示，以紅字提示使用者目前如何操作
	v3.2 : 加長顯示訊息，使比較長的訊息能完整顯示
	v3.1 : 修正no emotion detected會導致程式crash的問題，現在會請使用者重新上傳圖片
	v3.0 : 新增自選題目於右上角
	v2.3 : 移除主畫面右上角多餘的選單
	v2.2 : 新增使用者提示訊息
	v2.1 : 加強防呆防笨防蠢防低能防智障的預防機制
	v2.0 : 於右上角新增可以更換題目的按鍵
	v1.2 : 不該按的按鍵現在都不能按
	v1.1 : 新增防呆防笨防蠢防低能防智障的預防機制
	v1.0 : 修正剩下所有bug
```
