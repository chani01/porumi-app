import UIKit
import SwiftUI
import Shared

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Self.Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Self.Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
            // SNS 로그인 후 돌아오는 딥링크(porumi://login-callback)를 세션으로 바꾼다
            .onOpenURL { url in
                SocialLogin_iosKt.handleAuthDeeplink(url: url)
            }
    }
}