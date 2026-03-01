import UIKit
import SwiftUI
import ComposeApp


struct ComposeView: UIViewControllerRepresentable {

    private let controller = MainViewControllerKt.MainViewController()

    func makeUIViewController(context: Context) -> UIViewController {
        controller
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
    }
}



