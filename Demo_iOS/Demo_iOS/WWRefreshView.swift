//
//  WWRefreshView.swift
//  Demo_iOS
//
//  Created by 陈古松 on 2017/3/31.
//  Copyright © 2017年 cgspine. All rights reserved.
//

import UIKit


let ORIGIN_INSET: CGFloat = 12
let ANIMATION_DURATION: CFTimeInterval = 0.8
class WWRefreshView: SMRefreshHeader {
    var balls = [Ball]()
    var displayLink: CADisplayLink?
    var startAnimateTime: CFTimeInterval = 0
    override func prepare() {
        super.prepare()
        sm_width = SMRefreshHeaderHeight
        let size = min(sm_width, sm_height)
        let op1 = OriginPoint(sm_width/2, sm_height/2 - size / 2 + ORIGIN_INSET)
        let op2 = OriginPoint(center.x - size / 2 + ORIGIN_INSET, center.y)
        let op3 = OriginPoint(center.x,  center.y + size / 2  - ORIGIN_INSET)
        let op4 = OriginPoint(center.x + size / 2 - ORIGIN_INSET, center.y)
        op1.next = op2
        op2.next = op3
        op3.next = op4
        op4.next = op1
        
        
        let ballRadius: CGFloat = 3.5
        let ballSmallRadius: CGFloat = 1.5

        balls.append(Ball(ballRadius, ballSmallRadius, UIColor.red, op1))
        balls.append(Ball(ballRadius, ballSmallRadius, UIColor.blue, op2))
        balls.append(Ball(ballRadius, ballSmallRadius, UIColor.brown, op3))
        balls.append(Ball(ballRadius, ballSmallRadius, UIColor.darkGray, op4))
        
        balls.forEach{
            self.layer.addSublayer($0.layer)
            $0.draw()
        }
        displayLink = CADisplayLink(target: self, selector: #selector(displayLinkSelector))
        displayLink?.isPaused = true
        displayLink?.add(to: RunLoop.current, forMode: .commonModes)
        
    }
    
    func displayLinkSelector(){
        let now = CACurrentMediaTime()
        let percent: CGFloat = CGFloat((now - startAnimateTime) / ANIMATION_DURATION)
        balls.forEach{
            $0.calculate(percent: percent)
            $0.draw()
        }
        print("displayLinkSelector: \(percent)")
        setNeedsDisplay()
        if(percent > 1){
            startAnimateTime = now
            balls.forEach{
                $0.next()
            }
        }
    }
    
    
    override func notifyStateChange(_ oldState:RefreshState, newState:RefreshState) {
        if newState == .refreshing {
            startRefreshAnimation()
        }
        setNeedsDisplay()
        super.notifyStateChange(oldState, newState: newState)
    }
    
    override func scrollViewContentOffsetDidChange(_ change: [NSKeyValueChangeKey : Any]?){
        super.scrollViewContentOffsetDidChange(change)
        if self.state == .idle || self.state == .pulling || self.state == .overPulling {
            balls.forEach{
                $0.calculate(percent: self.pullingPercent)
                $0.draw()
            }
            setNeedsDisplay()
        }
    }
    
    func startRefreshAnimation(){
        print("start refresh animation")
        startAnimateTime = CACurrentMediaTime()
        self.displayLink?.isPaused = false
    } 
    
    override func endRefresh() {
        super.endRefresh()
        self.displayLink?.isPaused = true
    }

}

class OriginPoint {
    var x: CGFloat
    var y: CGFloat
    var next: OriginPoint?
    
    init(_ x: CGFloat, _ y: CGFloat){
        self.x = x 
        self.y = y
    }
}

class Ball{
    var radius:CGFloat
    var smallRadius: CGFloat
    var op: OriginPoint
    var color: UIColor
    var layer: CAShapeLayer
    
    var smallX: CGFloat = 0
    var smallY: CGFloat = 0
    var x: CGFloat = 0
    var y: CGFloat = 0

    init(_ radius: CGFloat, _ smallRadius: CGFloat, _ color: UIColor, _ op:OriginPoint){
        self.radius = radius
        self.smallRadius = smallRadius
        self.color = color
        self.op = op
        self.layer = CAShapeLayer()
        self.smallX = op.x
        self.x = op.x
        self.smallY = op.y
        self.y = op.y
    }
    
    func next() {
        self.op = self.op.next!
    }
    
    
    func drawOverCircle() {
        if(smallRadius > radius){
            self.layer.path = UIBezierPath(ovalIn: CGRect(x: self.smallX - self.smallRadius, y: self.smallY-self.smallRadius, width: 2 * self.smallRadius, height: 2 * self.smallRadius)).cgPath
        }else{
            self.layer.path = UIBezierPath(ovalIn: CGRect(x: self.x - self.radius, y: self.y-self.radius, width: 2 * self.radius, height: 2 * self.radius)).cgPath
        }
    }
    
    func calculate(percent: CGFloat){
        let innerPercent = percent > 1 ? 1 : percent
        let v: CGFloat = 1.3
        let smallChangePoint: CGFloat = 0.5, smallV1: CGFloat = 0.3
        let smallV2 = (1 - smallChangePoint * smallV1) / (1 - smallChangePoint)
        let ev = min(1, v * innerPercent);
        var smallEv: CGFloat
        if (innerPercent > smallChangePoint) {
            smallEv = smallV2 * (innerPercent - smallChangePoint) + smallChangePoint * smallV1
        } else {
            smallEv = smallV1 * innerPercent
        }
        
        
        let startX = op.x
        let startY = op.y
        let next = op.next!
        let endX = next.x
        let endY = next.y
        let f = (endY - startY) / (endX - startX)
        
        self.x = startX + (endX - startX) * ev
        self.y = f * (self.x - startX) + startY
        self.smallX = startX + (endX - startX) * smallEv
        self.smallY = f * (self.smallX - startX) + startY
    }
    
    func draw(){
        if smallX == x && smallY == y {
            drawOverCircle()
            return
        }
        
        /* 三角函数求四个点 */
        var angle: CGFloat
        var x1, y1, smallX1, smallY1, x2, y2, smallX2, smallY2: CGFloat
        if (self.smallX == self.x) {
            let v = (self.radius - self.smallRadius) / (self.y - self.smallY)
            if (v > 1 || v < -1) {
                drawOverCircle()
                return
            }
            angle = asin(v)
            let sinValue = sin(angle)
            let cosValue = cos(angle)
            x1 = self.x - radius * cosValue
            y1 = self.y - radius * sinValue
            x2 = self.x + radius * cosValue
            y2 = y1
            smallX1 = self.smallX - self.smallRadius * cosValue
            smallY1 = self.smallY - self.smallRadius * sinValue
            smallX2 = self.smallX + self.smallRadius * cosValue
            smallY2 = self.smallY
        } else if (self.smallY == self.y) {
            let v = (self.radius - self.smallRadius) / (self.x - self.smallX);
            if (v > 1 || v < -1) {
                drawOverCircle()
                return
            }
            angle = asin(v)
            let sinValue = sin(angle)
            let cosValue = cos(angle)
            x1 = self.x - radius * sinValue
            y1 = self.y + radius * cosValue
            x2 = x1
            y2 = self.y - radius * cosValue
            smallX1 = self.smallX - self.smallRadius * sinValue
            smallY1 = self.smallY + self.smallRadius * cosValue
            smallX2 = smallX1
            smallY2 = self.smallY - self.smallRadius * cosValue
        } else {
            let ab = sqrt(pow(y - smallY, 2) + pow(x - smallX, 2))
            let v = (radius - smallRadius) / ab
            if (v > 1 || v < -1) {
                drawOverCircle()
                return
            }
            let alpha = asin(v)
            let b = atan((smallY - y) / (smallX - x))
            angle = CGFloat(M_PI / 2) - alpha - b
            var sinValue = sin(angle)
            var cosValue = cos(angle)
            smallX1 = smallX - smallRadius * cosValue
            smallY1 = smallY + smallRadius * sinValue
            x1 = x - radius * cosValue
            y1 = y + radius * sinValue
            
            angle = b - alpha
            sinValue = sin(angle)
            cosValue = cos(angle)
            smallX2 = smallX + smallRadius * sinValue
            smallY2 = smallY - smallRadius * cosValue
            x2 = x + radius * sinValue
            y2 = y - radius * cosValue
            
        }
        
        /* 控制点 */
        let centerX = (x + smallX) / 2, centerY = (y + smallY) / 2
        let center1X = (x1 + smallX1) / 2, center1y = (y1 + smallY1) / 2
        let center2X = (x2 + smallX2) / 2, center2y = (y2 + smallY2) / 2
        let k1 = (center1y - centerY) / (center1X - centerX)
        let k2 = (center2y - centerY) / (center2X - centerX)
        let ctrlV: CGFloat = 0.08
        let anchor1X = center1X + (centerX - center1X) * ctrlV, anchor1Y = k1 * (anchor1X - center1X) + center1y
        let anchor2X = center2X + (centerX - center2X) * ctrlV, anchor2Y = k2 * (anchor2X - center2X) + center2y
        
        let cutePath = UIBezierPath()
        // what a fuck? 
        // 通过顺时针、逆时针绘制path与appendPath组合会造成重叠区域镂空的效果，这里要避免
        if(smallX < x){
            let pointStart = CGPoint(x: smallX1, y: smallY1)
            cutePath.move(to: pointStart)
            cutePath.addLine(to: CGPoint(x: smallX2, y: smallY2))
            cutePath.addQuadCurve(to: CGPoint(x: x2, y: y2), controlPoint: CGPoint(x: anchor2X, y: anchor2Y))
            cutePath.addLine(to: CGPoint(x: x1, y: y1))
            cutePath.addQuadCurve(to: pointStart, controlPoint: CGPoint(x: anchor1X, y: anchor1Y))
        }else{
            let pointStart = CGPoint(x: smallX2, y: smallY2)
            cutePath.move(to: pointStart)
            cutePath.addLine(to: CGPoint(x: smallX1, y: smallY1))
            cutePath.addQuadCurve(to: CGPoint(x: x1, y: y1), controlPoint: CGPoint(x: anchor1X, y: anchor1Y))
            cutePath.addLine(to: CGPoint(x: x2, y: y2))
            cutePath.addQuadCurve(to: pointStart, controlPoint: CGPoint(x: anchor2X, y: anchor2Y))
        }
        
        let circle1 = UIBezierPath(ovalIn: CGRect(x: self.smallX - self.smallRadius, y: self.smallY-self.smallRadius, width: 2 * self.smallRadius, height: 2 * self.smallRadius))
        cutePath.append(circle1)
        let circle2 = UIBezierPath(ovalIn: CGRect(x: self.x - self.radius, y: self.y-self.radius, width: 2 * self.radius, height: 2 * self.radius))
        cutePath.append(circle2)
    
        layer.path = cutePath.cgPath
        self.layer.fillColor = color.cgColor
    }
}

extension WWRefreshView: CAAnimationDelegate {
    func animationDidStart(_ anim: CAAnimation){
        
    }
    
    func animationDidStop(_ anim: CAAnimation, finished flag: Bool){
        
    }
}
