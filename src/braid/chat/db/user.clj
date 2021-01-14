(ns braid.chat.db.user
  (:require
    [clojure.edn :as edn]
    [clojure.string :as string]
    [crypto.password.scrypt :as password]
    [datomic.api :as d]
    [braid.core.common.util :refer [slugify]]
    [braid.core.hooks :as hooks]
    [braid.core.server.db :as db]
    [braid.chat.db.common :refer :all]
    [braid.lib.gravatar :as gravatar]))

(defn email-taken?
  [email]
  (some? (d/entity (db/db) [:user/email email])))

(defn nickname-taken?
  [nickname]
  (some? (d/entity (db/db) [:user/nickname nickname])))

(defn generate-unique-nickname
  "Recursively checks if nickname is taken; otherwise appends a random number and repeats"
  [potential-nickname]
  (if (nickname-taken? potential-nickname)
    (generate-unique-nickname (str potential-nickname (rand-int 9)))
    potential-nickname))

(defn generate-nickname-from-email
  "Generates a nickname from an email string"
  [email]
  (-> email
      (string/split #"@")
      first
      slugify
      generate-unique-nickname))

(defn authenticate-user
  "returns user-id if email and password are correct"
  [email password]
  (->> (let [[user-id password-token]
             (d/q '[:find [?id ?password-token]
                    :in $ ?email
                    :where
                    [?e :user/id ?id]
                    [?e :user/email ?stored-email]
                    [(.toLowerCase ^String ?stored-email) ?email]
                    [?e :user/password-token ?password-token]]
                  (db/db)
                  (string/lower-case email))]
         (when (and user-id (password/check password password-token))
           user-id))))

(defn user-by-id
  [id]
  (some-> (d/pull (db/db) private-user-pull-pattern [:user/id id])
          db->private-user))

(defn user-id-exists?
  [id]
  (some? (d/entity (db/db) [:user/id id])))

(defn user-with-email
  "Get the user with the given email address or nil if no such user registered"
  [email]
  (some-> (d/pull (db/db) user-pull-pattern [:user/email (string/lower-case email)])
          db->user))

(defn user-email
  "Get the email address for the user with the given id"
  [user-id]
  (:user/email (d/pull (db/db) [:user/email] [:user/id user-id])))

(defn user-get-preferences
  [user-id]
  (->> (d/pull (db/db)
               [{:user/preferences [:user.preference/key :user.preference/value]}]
               [:user/id user-id])
       :user/preferences
       (into {}
             (comp (map (juxt :user.preference/key :user.preference/value))
                   (map (fn [[k v]] [k (edn/read-string v)]))))))

(defn user-get-preference
  [user-id pref]
  (some-> (d/q '[:find ?val .
                 :in $ ?user-id ?key
                 :where
                 [?u :user/id ?user-id]
                 [?u :user/preferences ?p]
                 [?p :user.preference/key ?key]
                 [?p :user.preference/value ?val]]
               (db/db) user-id pref)
          edn/read-string))

(defn user-preference-is-set?
  "If the preference with the given key has been set for the user, return the
  entity id, else nil"
  [user-id pref]
  (d/q '[:find ?p .
         :in $ ?user-id ?key
         :where
         [?u :user/id ?user-id]
         [?u :user/preferences ?p]
         [?p :user.preference/key ?key]]
       (db/db) user-id pref))

(defn user-search-preferences
  "Find the ids of users that have the a given value for a given key set in
  their preferences"
  [k v]
  (d/q '[:find [?user-id ...]
         :in $ ?k ?v
         :where
         [?u :user/id ?user-id]
         [?u :user/preferences ?pref]
         [?pref :user.preference/key ?k]
         [?pref :user.preference/value ?v]]
       (db/db)
       k (pr-str v)))

(defn users-for-user
  "Get all users visible to given user"
  [user-id]
  (->> (d/q '[:find (pull ?e pull-pattern)
              :in $ ?user-id pull-pattern
              :where
              [?u :user/id ?user-id]
              [?g :group/user ?u]
              [?g :group/user ?e]]
            (db/db)
            user-id
            user-pull-pattern)
       (map (comp db->user first))
       set))

(defn user-visible-to-user?
  "Are the two user ids users that can see each other? i.e. do they have at least one group in common"
  [user1-id user2-id]
  (-> (d/q '[:find ?g
             :in $ ?u1-id ?u2-id
             :where
             [?u1 :user/id ?u1-id]
             [?u2 :user/id ?u2-id]
             [?g :group/user ?u1]
             [?g :group/user ?u2]]
           (db/db) user1-id user2-id)
      seq boolean))

;; Transactions

(defonce post-create-txns
  (hooks/register! (atom []) [fn?]))

(defn create-user-txn
  "given an id and email, creates and returns a user;
  the nickname and avatar are set based on the email;
  the id, email, and resulting nickname must be unique"
  [{:keys [id email]}]
  (db/assert (not (user-id-exists? id))
             (format "User with id %s already exists" id))
  (let [new-id (d/tempid :entities)]
    (into
      [^{:braid.core.server.db/return
         (fn [{:keys [db-after tempids]}]
           (->> (d/resolve-tempid db-after tempids new-id)
                (d/entity db-after)
                db->private-user))}
       {:db/id new-id
        :user/id id
        :user/email (string/lower-case email)
        :user/avatar (gravatar/url email
                                   :rating :g
                                   :default :identicon)
        :user/nickname (generate-nickname-from-email email)}]
      (mapcat #(% new-id) @post-create-txns))))

(defn set-nickname-txn
  "Set the user's nickname"
  [user-id nickname]
  [[:db/add [:user/id user-id] :user/nickname (slugify nickname)]])

(defn set-user-avatar-txn
  [user-id avatar]
  [[:db/add [:user/id user-id] :user/avatar avatar]])

(defn set-user-password-txn
  [user-id password]
  [[:db/add [:user/id user-id] :user/password-token (password/encrypt password)]])

(defn user-set-preference-txn
  "Set a key to a value for the user's preferences."
  [user-id k v]
  (if-let [e (user-preference-is-set? user-id k)]
    [[:db/add e :user.preference/value (pr-str v)]]
    [{:user.preference/key k
      :user.preference/value (pr-str v)
      :user/_preferences [:user/id user-id]
      :db/id (d/tempid :entities)}]))
