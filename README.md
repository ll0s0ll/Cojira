# Cojira
CojiraはBlackBerryでradikoを聴取するためのアプリケーションです。個人的に使うために作成しましたが、風前の灯ともいえるBlackBerryを使い続ける数少ない同士のために、こっそり公開します。非公式アプリですので、使われる方は、こっそりでお願いします。怒られたら取り下げる所存でございます。

個人的に我慢できる程度にしか作っていません。調子が悪くなったら、アプリを再起動するというスタンスでお願いします。アプリケーション起動後、放送局の情報が出るまで少し時間がかかります。失敗したらダイアログを出すようにしていますが、あまりに時間がかかるようでしたら、再起動させてください。

**2014年4月より開始された、radiko.jpプレミアム（エリアフリー聴取）には対応しておりません。また、2016年10月より開始された、タイムフリー聴取機能には対応しておりません。**

**※ご注意※**  
本ソフトウエアはradiko社と一切関係がありませんので、問い合わせ等は絶対にしないでください。また、本ソフトウエアを使用して発生した、物理的な破損、データ損失、金銭的な損失等、いかなる損害について一切責任を負いません。本ソフトウエアは無償で提供、また、ソースコードも公開しますので、ユーザーサポート、バージョンアップ及び修正要求への対応はいたしません。ご理解をいただいた上でご利用ください。

## 動作環境
私が使っているBlackBerry Bold 9900(7.1.0.523)のみで動作確認をしています（というか実機デバッグです涙）。同じ機種、OSでも動作しない場合もあるかと思います。7.0.0のSDKを使って開発したので、OS6では動作しません。9900以外の機種でもOS7以降がインストールされていれば動くかもしれません。OS10には対応していません。

BISはエリア外と判定されるため使用できません。Wifi接続が必須になります。（APNを設定すれば3G回線でも使用できますが、高額な通信費用を請求される恐れがあるため、熟知されている方以外は控えられたほうが良いと思います。）

## ダウンロード
OTA（Over-The-Air）のみで提供しています。下記リンクをBlackBerryの標準ブラウザからクリックすると、確認画面の後、インストールされます。確認画面でアプリケーション名を確認の上、ダウンロードをクリックしてください。インストールの前には、必ずお使いのBlackBerryのバックアップを取ってください。

[Cojira 0.4](https://ll0s0ll.github.io/blackberry/cojira/bin/Cojira.jad)

## バージョン情報
ver 0.4 (2017/04/12)
- 仕様変更に対応。([Issue #3](https://github.com/ll0s0ll/Cojira/issues/3))

ver 0.3 (2015/06/29)
- マイナーバグフィックス（コード書き直し）

ver 0.2 (2014/09/13)
- ホスト名変更に対応。([Issue #2](https://github.com/ll0s0ll/Cojira/issues/2))

ver 0.1 (2014/01/26)
- 公開

## スクリーンショット
![メイン画面](https://ll0s0ll.github.io/blackberry/cojira/img/cojira_mainscreen.jpg "メイン画面")  
<sub>[メイン画面]</sub>

![番組情報画面](https://ll0s0ll.github.io/blackberry/cojira/img/cojira_progscreen.jpg "番組情報画面")  
<sub>[番組情報画面]</sub>

![タイムテーブル画面](https://ll0s0ll.github.io/blackberry/cojira/img/cojira_timetablescreen.jpg "タイムテーブル画面")  
<sub>[タイムテーブル画面]</sub>

