うろんころん
============

"うろんころん"は、キューイングなしにアプリケーション間でメッセージを送るためのシステムです。

メッセージキューは便利な仕組みですが、小規模なアプリケーションではそこまでのものは
必要ないことがあります。
また、異なる部署間や会社間をつなごうとしたときに、キューイングの仕組みが誰の持ち物なんだ、
誰が面倒みるんだ、という点で揉めることがあります。

「メッセージキューを必要とせず、手軽にデータを送りたい」

そんなニーズに応えるためのメッセージングの仕組みです。


## 使い方

### Producer

データを送信する側です。

```clojure
(use '[ulon-colon.producer])
```

```clojure
(start-publisher 5000)
```

これで、データ受信するアプリケーション(Consumer)が、このポートで接続できるようになります。
接続しているConsumerにデータを送るには、次のようにします。

```clojure
(produce "Hello World!")
```

実用的にはConsumer側がうまく受け取れたかどうかを知りたいことがあるでしょう。
その場合は、

```clojure
(if (= @(lamina.core/read-channel (produce "Hello World!")) :commit)
  (成功時の処理)
  (失敗時の処理))
```

と、やってあげることでConsumer側の成否に応じた処理を書き分けることができます。

### Consumer

データを受信する側です。make-consumerでProducerに接続します。

```clojure
(use '[ulon-colon.producer])

(def consumer (make-consumer "ws://localhost:5000"))
```

データの受け取りは、同期と非同期の両方が可能です。

非同期型は次のように、コールバック関数を登録します。

```clojure
(consume consumer コールバック関数)
```

こうしておくことでメッセージを受信した瞬間に、コールバック関数が呼ばれます。

同期型も同じくコールバック関数が呼ばれますが、メッセージを受信するまで処理がブロックされます。

```clojure
(consume-sync consumer コールバック関数)
```

### 送受信可能なデータ

文字列だけでなく、Clojure のデータ型であれば、すべてそのまま送受信可能です。

```clojure
(produce {:a [1 2 3] :b #{"A" "B" "C"}})
```

と送ってやれば、

```clojure
=> (consume-sync consumer println)
{:a #<ArrayList [1, 2, 3]>, :b #<HashSet [A, B, C]>}
```

と受信出来ます。

VectorがArrayListに、SetがHashSetになってしまうのは、data.fressianの現在の仕様に
よるものです。そのうちネイティブなClojureの値になると思われます。

## アーキテクチャ

ProducerがWebSocketサーバ、ConsumerがWebSocketクライアントとして動作することで、リアルタイムなデータの転送を可能にしています。
転送データはFressianを使ってシリアライズしています。


## License

Copyright © 2014 kawasima

Distributed under the Eclipse Public License, the same as Clojure.
