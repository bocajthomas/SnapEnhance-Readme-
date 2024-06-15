interface Class<T> {
    getName(): string;
}

interface JavaType {
    /** @deprecated */
    newInstance(...args: any[]): any;
    __new__(...args: any[]): any;
}

interface SEWrapper {
    instanceNonNull(): any;
    isPresent(): boolean;
    toString(): string;

    getEnumValue(fieldName: string, defaultValue: any): any;
    setEnumValue(fieldName: string, value: any /* java.lang.Enum */): void;
}

interface JSConsole {
    log(...data: any): void;
    warn(...data: any): void;
    error(...data: any): void;
    debug(...data: any): void;
    info(...data: any): void;
    trace(...data: any): void;
    verbose(...data: any): void;
}

declare const console: JSConsole;

declare const currentSide: "core" | "manager";

declare namespace module {
    interface ModuleInfo {
        readonly name: string;
        readonly displayName: string;
        readonly version: string;
        readonly description: string | undefined;
        readonly author: string | undefined;
        readonly minSnapchatVersion: number | undefined;
        readonly minSEVersion: number | undefined;
        readonly grantedPermissions: string[];
    }

    let exports: any | undefined;

    const info: ModuleInfo;

    // SnapEnhance side
    let onSnapEnhanceLoad: ((context: any) => void) | undefined;

    // Snapchat side
    let onSnapApplicationLoad: ((context: any) => void) | undefined;
    let onSnapMainActivityCreate: ((activity: any) => void) | undefined;

    let onUnload: (() => void) | undefined;
}

declare function logInfo(message: any);
declare function logError(message: any, throwable?: any);

declare function shortToast(...messages: string[]);
declare function longToast(...messages: string[]);

declare function type(className: string, useModClassLoader?: boolean): JavaType | undefined;
declare function findClass(className: string, useModClassLoader?: boolean): Class<any> | undefined;
declare function setField(instance: any, fieldName: string, value: any | undefined): void;
declare function getField(instance: any, fieldName: string): any | undefined;

declare function sleep(durationMs: number);

declare module "hooker" {
    enum stage {
        BEFORE = "before",
        AFTER = "after"
    }

    interface ScriptHookCallback {
        result: any;
        readonly thisObject: any;
        readonly method: any;
        readonly args: any[];

        cancel(): void;
        arg(index: number): any;
        setArg(index: number, value: any): void;
        invokeOriginal(): void;
        invokeOriginal(args: any[]): void;
        toString(): string;
    }

    type HookCallback = (scriptHookCallback: ScriptHookCallback) => void;
    type HookUnhook = () => void;
    
    function findMethod(clazz: Class<any>, methodName: string): any | undefined;
    function findMethodWithParameters(clazz: Class<any>, methodName: string, types: string[]): any | undefined;
    function findMethod(className: string, methodName: string): any | undefined;
    function findMethodWithParameters(className: string, methodName: string, types: string[]): any | undefined;
    function findConstructor(clazz: Class<any>, types: string[]): any | undefined;
    function findConstructorParameters(className: string, types: string[]): any | undefined;

    function hook(method: any, stage: stage, callback: HookCallback): HookUnhook;
    function hookAllMethods(clazz: Class<any>, methodName: string, stage: stage, callback: HookCallback): HookUnhook;
    function hookAllConstructors(clazz: Class<any>, stage: stage, callback: HookCallback): HookUnhook;
    function hookAllMethods(className: string, methodName: string, stage: stage, callback: HookCallback): HookUnhook | undefined;
    function hookAllConstructors(className: string, stage: stage, callback: HookCallback): HookUnhook | undefined;
}

declare module "config" {
    function get(key: string): string | undefined;
    function get(key: string, defaultValue: any): string;

    function getInteger(key: string): number | undefined;
    function getInteger(key: string, defaultValue: number): number;

    function getDouble(key: string): number | undefined;
    function getDouble(key: string, defaultValue: number): number;

    function getBoolean(key: string): boolean | undefined;
    function getBoolean(key: string, defaultValue: boolean): boolean;

    function getLong(key: string): number | undefined;
    function getLong(key: string, defaultValue: number): number | undefined;

    function getFloat(key: string): number | undefined;
    function getFloat(key: string, defaultValue: number): number | undefined;

    function getByte(key: string): number | undefined;
    function getByte(key: string, defaultValue: number): number | undefined;

    function getShort(key: string): number | undefined;
    function getShort(key: string, defaultValue: number): number | undefined;

    function set(key: string, value: any): void;
    function set(key: string, value: any, save: boolean): void;

    function setInteger(key: string, value: number): void;
    function setInteger(key: string, value: number, save: boolean): void;

    function setDouble(key: string, value: number): void;
    function setDouble(key: string, value: number, save: boolean): void;

    function setBoolean(key: string, value: boolean): void;
    function setBoolean(key: string, value: boolean, save: boolean): void;

    function setLong(key: string, value: number): void;
    function setLong(key: string, value: number, save: boolean): void;

    function setFloat(key: string, value: number): void;
    function setFloat(key: string, value: number, save: boolean): void;

    function setByte(key: string, value: number): void;
    function setByte(key: string, value: number, save: boolean): void;

    function setShort(key: string, value: number): void;
    function setShort(key: string, value: number, save: boolean): void;

    function save(): void;
    function load(): void;
    function deleteConfig(): void;
}

declare module "interface-manager" {
    type enumUi = "settings" | "friendFeedContextMenu" | "conversationToolbox"

    interface AlertDialog {
        show(): void;
        dismiss(): void;
    }

    type BuilderCallback = (builder: InterfaceBuilder, args: Record<string, any>) => void;
    type AlertDialogCallback = (builder: InterfaceBuilder, alertDialog: AlertDialog) => void;

    interface Node {
        setAttribute(key: string, value: any | undefined): void
        fillMaxWidth(): Node
        fillMaxHeight(): Node
        label(text: string): Node
        padding(padding: number): Node
        fontSize(size: number): Node
        color(color: number): Node
        visibility(visibility: "visible" | "invisible" | "gone"): Node
    }

    interface RowColumnNode extends Node {
        arrangement(arrangement: "start" | "end" | "top" | "bottom" | "center" | "spaceBetween" | "spaceAround" | "spaceEvenly"): RowColumnNode
        alignment(alignment: "start" | "end" | "top" | "bottom" | "centerVertically" | "centerHorizontally"): RowColumnNode
        spacedBy(spacing: number): RowColumnNode
    }

    interface TextInputNode extends Node {
        value(value: string): TextInputNode
        placeholder(placeholder: string): TextInputNode
        callback(callback: ((value: string) => void)): TextInputNode
        readonly(state: boolean): TextInputNode
        singleLine(state: boolean): TextInputNode
        maxLines(lines: number): TextInputNode
    }

    interface InterfaceBuilder {
        onDispose(callback: (() => void)): void;
        onLaunched(callback: (() => void)): void;
        onLaunched(key: any, callback: (() => void));
        row(callback: BuilderCallback): RowColumnNode;
        column(callback: BuilderCallback): RowColumnNode;
        text(label: string): Node;
        switch(state: boolean | undefined, onChange: ((state: boolean) => void)): Node;
        button(label: string, onClick: (() => void)): Node;
        slider(min: number, max: number, step: number, value: number, onChange: ((value: number) => void)): Node;
        list(label: string, items: string[], onClick: ((index: number) => void)): Node;
        textInput(placeholder: string, value: string, onChange: ((value: string) => void)): TextInputNode;
    }

    function create(name: enumUi, callback: BuilderCallback): void;
    function createAlertDialog(activity: any, callback: AlertDialogCallback): AlertDialog;
    function createAlertDialog(activity: any, builder: ((alertDialogBuilder: any) => void), callback: AlertDialogCallback): AlertDialog;
}

declare module "ipc" {
    type Listener = (args: any[]) => void;

    function on(channel: string, listener: Listener): void;
    function onBroadcast(channel: string, listener: Listener): void;

    function emit(eventName: string): void;
    function emit(eventName: string, ...args: any[]): void;
    function broadcast(channel: string, eventName: string): void;
    function broadcast(channel: string, eventName: string, ...args: any[]): void;
}

declare module "java-interfaces" {
    function runnable(callback: (() => void)): any;
    function newProxy(javaClass: Class<any>, callback: ((proxy: any, method: any, args: any[]) => any)): any;
}

declare module "messaging" {
    interface SnapUUID extends SEWrapper {
        toBytes(): any; // byte[]
        toUUID(): any; // java.util.UUID
    }

    interface MessageContent extends SEWrapper {
        content: any; // byte[]
        contentType: any;
    }

    interface UserIdToReaction {
        userId: SnapUUID;
        reactionId: number;
    }

    interface MessageMetadata extends SEWrapper {
        createdAt: number,
        readAt: number,
        playableSnapState: any;
        savedBy: SnapUUID[];
        openedBy: SnapUUID[];
        seenBy: SnapUUID[];
        reactions: UserIdToReaction[];
        isSaveable: boolean;
    }

    interface MessageDescriptor extends SEWrapper {
        messageId: number;
        conversationId: SnapUUID;
    }

    interface Message extends SEWrapper {
        orderKey: number;
        senderId: SnapUUID;
        messageContent: MessageContent;
        messageMetadata: MessageMetadata;
        messageDescriptor: MessageDescriptor;
        messageState: any

        serialize(): string?;
    }

    type ResultCallback = (error?: any) => void;
    type MessageResultCallback = (error?: any, message?: Message) => void;
    type MessageListResultCallback = (error?: any, messages: Message[]) => void;

    interface ConversationUserIdPair {
        readonly conversationId: string;
        readonly userId: string;
    }

    type MessageUpdate =  "read" | "release" | "save" | "unsave" | "erase" | "screenshot" | "screen_record" | "replay" | "reaction" | "remove_reaction" | "revoke_transcription" | "allow_transcription" | "erase_saved_story_media";


    function isPresent(): boolean;
    function newSnapUUID(uuid: string): SnapUUID;

    function updateMessage(conversationId: string, messageId: number, action: MessageUpdate, callback: ResultCallback): void;
    function fetchConversationWithMessagesPaginated(conversationId: string, lastMessageId: number, amount: number, callback: MessageListResultCallback): void;
    function fetchConversationWithMessages(conversationId: string, callback: MessageListResultCallback): void;
    function fetchMessageByServerId(conversationId: string, serverId: number, callback: MessageResultCallback): void;
    function fetchMessagesByServerIds(conversationId: string, serverIds: number[], callback: MessageListResultCallback): void;
    function displayedMessages(conversationId: string, lastMessageId: number, callback: ResultCallback): void;
    function fetchMessage(conversationId: string, messageId: number, callback: MessageResultCallback): void;
    function clearConversation(conversationId: string, callback: ResultCallback): void;
    function getOneOnOneConversationIds(userIds: string[], callback: (error?: any, result?: ConversationUserIdPair[]) => void): void;
    function sendChatMessage(conversationId: string, message: string, callback: ResultCallback): void;

    interface BitmojiInfo extends SEWrapper {
        avatarId?: string
        backgroundId?: string
        sceneId?: string
        selfieId?: string
    }

    interface Snapchatter extends SEWrapper {
        readonly bitmojiInfo?: BitmojiInfo
        displayName?: string
        userId: SnapUUID
        username: string
    }

    function fetchSnapchatterInfos(userIds: string[]): Snapchatter[];
}



declare module "networking" {
    interface RequestBuilder {
        url(url: string): RequestBuilder;
        addHeader(name: string, value: string): RequestBuilder;
        removeHeader(name: string): RequestBuilder;
        method(method: "POST" | "GET" | "PUT" | "HEAD" | "DELETE" | "PATCH", body: string | any | undefined /* byte[] | java.io.InputStream | null */): RequestBuilder;
    }

    interface Response {
        readonly statusCode: number;
        readonly statusMessage: string;
        readonly headers: Record<string, string>;
        readonly bodyAsString: string;
        readonly bodyAsStream: any; // java.io.InputStream
        readonly bodyAsByteArray: any; // byte[]
        readonly contentLength: number;
        getHeader(name: string): string | undefined;
        close(): void;
    }

    interface Websocket {
        cancel(): void;
        close(code: number, reason: string): void;
        queueSize(): number;
        send(bytes: any): void; // byte[] | string
    }

    interface WebsocketListener {
        onOpen(websocket: Websocket, response: Response): void;
        onClosed(websocket: Websocket, code: number, reason: string): void;
        onClosing(websocket: Websocket, code: number, reason: string): void;
        onFailure(websocket: Websocket, throwable: any, response: Response | undefined): void;
        onMessageBytes(websocket: Websocket, bytes: any): void; // byte[]
        onMessageText(websocket: Websocket, text: string): void;
    }

    function getUrl(url: string, callback: (error: string | undefined, response: string) => void): void;
    function getUrlAsStream(url: string, callback: (error: string | undefined, response: any) => void): void; // java.io.InputStream
    function newRequest(): RequestBuilder;
    function enqueue(requestBuilder: RequestBuilder, callback: (error: string | undefined, response: Response | undefined) => void): void;
    function execute(requestBuilder: RequestBuilder): Response;
    function newWebSocket(requestBuilder: RequestBuilder, listener: WebsocketListener): void;
}

declare module "events" {
    interface Event {
        canceled: boolean;
    }

    interface ConversationUpdateEvent extends Event {
        readonly conversationId: string;
        readonly conversation: any; // com.snapchat.client.messaging.Conversation
        readonly messages: Message[];
    }

    interface BuildMessageEvent extends Event {
        readonly message: Message;
    }

    interface BindViewEvent extends Event {
        readonly model: any
        readonly view: any; // android.view.View
    }

    interface OnSnapInteractionEvent extends Event {
        readonly interactionType: any; // enum
        readonly conversationId: string;
        readonly messageId: number; // long
    }

    interface SendMessageWithContentEvent extends Event {
        readonly destinations: MessageDestinations;
        readonly messageContent: MessageContent;
    }

    interface AddViewEvent extends Event {
        readonly parent: any; // android.view.ViewGroup
        view: any; // android.view.View
    }

    function onConversationUpdated(callback: (event: ConversationUpdateEvent) => void): void;
    function onMessageBuild(callback: (event: BuildMessageEvent) => void): void;
    function onViewBind(callback: (event: BindViewEvent) => void): void;
    function onSnapInteraction(callback: (event: OnSnapInteractionEvent) => void): void;
    function onPreMessageSend(callback: (event: SendMessageWithContentEvent) => void): void;
    function onAddView(callback: (event: AddViewEvent) => void): void;
}
