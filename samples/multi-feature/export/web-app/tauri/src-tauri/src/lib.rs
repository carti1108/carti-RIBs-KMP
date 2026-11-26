/// 웹 샘플을 창 하나에 띄운다. 할 일 목록은 같은 출처의 `/api/todos`(react/public)에서 읽는다.
#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
